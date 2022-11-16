package net.tjalp.nautilus.player.profile

import com.destroystokyo.paper.event.player.PlayerConnectionCloseEvent
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import net.tjalp.nautilus.Nautilus
import net.tjalp.nautilus.database.MongoCollections
import net.tjalp.nautilus.event.ProfileUpdateEvent
import net.tjalp.nautilus.util.player
import net.tjalp.nautilus.util.profile
import net.tjalp.nautilus.util.register
import net.tjalp.nautilus.util.skin
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.AsyncPlayerPreLoginEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.litote.kmongo.coroutine.toList
import org.litote.kmongo.reactivestreams.findOneById
import org.litote.kmongo.reactivestreams.save
import org.litote.kmongo.regex
import org.litote.kmongo.setValue
import java.time.LocalDateTime
import java.util.*

/**
 * The profile manager manages everything about
 * profiles and everything they contain, like
 * prefixes, suffixes, ranks, roles etc.
 */
class ProfileManager(
    private val nautilus: Nautilus
) {

    /** The profile cache that can be accessed via [cacheProfile] */
    private val profileCache = HashMap<UUID, ProfileSnapshot>()

    /** The coroutine Mongo client */
    private val profiles = MongoCollections.profiles

    init {
        ProfileListener().register()
    }

    /**
     * Retrieve the latest [ProfileSnapshot] of a
     * cached player. If not cached, an error will
     * be thrown.
     *
     * @param player The player to retrieve the profile of
     * @return The associated & cached [ProfileSnapshot] of the specified player
     */
    fun profile(player: Player): ProfileSnapshot {
        return this.profileCache[player.uniqueId] ?: run {
            throw IllegalStateException("No profile present for ${player.uniqueId}")
        }
    }

    /**
     * Retrieve the latest [ProfileSnapshot] of an
     * (offline) user from their unique id.
     * This method is suspending, so handle it accordingly.
     *
     * @param uniqueId The unique identifier of the target user
     * @return The associated & cached [ProfileSnapshot] of the specified unique id
     */
    suspend fun profile(uniqueId: UUID): ProfileSnapshot? {
        val player = this.nautilus.server.getPlayer(uniqueId)

        if (player != null) return this.profile(player)

        return this.profiles.findOneById(uniqueId).awaitFirstOrNull()
    }

    /**
     * Retrieve the latest [ProfileSnapshot] of an
     * (offline) user from their username.
     * This method is suspending, so handle it accordingly.
     *
     * @param username The username of the target user
     * @return The associated & cached [ProfileSnapshot] of the specified username
     */
    suspend fun profile(username: String): ProfileSnapshot? {
        val player = this.nautilus.server.getPlayer(username)

        if (player != null) return this.profile(player)

        val profiles = this.profiles.find(
            ProfileSnapshot::lastKnownName regex Regex("^${username.escapeIfNeeded()}$", RegexOption.IGNORE_CASE)
        ).toList()

        if (profiles.size > 1) {
            this.nautilus.logger.warning("Multiple profiles were found for '${username}', requesting a unique id from Mojang!")

            val uniqueId = withContext(Dispatchers.IO) {
                nautilus.server.getPlayerUniqueId(username)
            } ?: return null

            return this.profile(uniqueId)
        }

        return profiles.firstOrNull()
    }

    /**
     * Create a new profile if no profile exists for
     * the target unique id
     *
     * @param uniqueId The unique id to create the profile
     * @param fill Whether to fill in other properties as well, such as the name and skin
     * @return The profile associated with the unique id, which is never null
     */
    suspend fun createProfileIfNonexistent(uniqueId: UUID, fill: Boolean = true): ProfileSnapshot {
        var profile = this.profile(uniqueId)

        if (profile == null) {
            profile = ProfileSnapshot(uniqueId)

            if (fill) {
                val playerProfile = this.nautilus.server.createProfile(uniqueId)

                withContext(Dispatchers.IO) {
                    playerProfile.complete(true)
                }

                val username = playerProfile.name
                val skin = playerProfile.skin()

                if (username != null) profile = profile.copy(lastKnownName = username)
                if (skin != null) profile = profile.copy(lastKnownSkin = skin)
            }

            this.profiles.save(profile).awaitSingle()
        }

        return profile
    }

    /**
     * Update a profile with a new one
     *
     * @param profile The new profile to update with
     */
    fun onProfileUpdate(profile: ProfileSnapshot) {
        var previous: ProfileSnapshot? = null

        if (profile.player()?.isOnline == true) {
            previous = cacheProfile(profile)
        }

        ProfileUpdateEvent(
            profile = profile,
            previous = previous
        ).callEvent()
    }

    /**
     * Cache a profile
     *
     * @param profile The profile to cache
     * @return The previous profile if exists, otherwise null
     */
    private fun cacheProfile(profile: ProfileSnapshot): ProfileSnapshot? {
        nautilus.logger.info("Caching the profile of ${profile.player()?.name} (${profile.uniqueId})")
        return this.profileCache.put(profile.uniqueId, profile)
    }

    /**
     * The inner profile listener class, which will
     * listen and cache profiles
     */
    private inner class ProfileListener : Listener {

        @EventHandler
        fun on(event: AsyncPlayerPreLoginEvent) {
            synchronized(this) {
                runBlocking {
                    cacheProfile(createProfileIfNonexistent(event.uniqueId, fill = false))
                }
            }
        }

        @EventHandler
        fun on(event: PlayerConnectionCloseEvent) {
            synchronized(this) {
                nautilus.logger.info("Removing the cached profile of ${event.playerName} (${event.playerUniqueId})")
                profileCache -= event.playerUniqueId
            }
        }

        @EventHandler(priority = EventPriority.LOW)
        fun on(event: PlayerJoinEvent) {
            val player = event.player
            val profile = player.profile()
            val playerProfile = player.playerProfile
            val skin = playerProfile.skin()

            nautilus.scheduler.launch {
                profile.update(
                    setValue(ProfileSnapshot::lastKnownName, player.name),
                    setValue(ProfileSnapshot::lastOnline, LocalDateTime.now()),
                    setValue(ProfileSnapshot::lastKnownSkin, skin)
                )
            }
        }

        @EventHandler(priority = EventPriority.LOW)
        fun on(event: PlayerQuitEvent) {
            val player = event.player
            val profile = player.profile()

            nautilus.scheduler.launch {
                profile.update(setValue(ProfileSnapshot::lastOnline, LocalDateTime.now()))
            }
        }
    }
}