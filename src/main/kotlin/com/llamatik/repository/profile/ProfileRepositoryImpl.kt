package com.llamatik.repository.profile

import com.llamatik.repository.DatabaseFactory.dbQuery
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update

class ProfileRepositoryImpl : ProfileRepository {

    override suspend fun addProfile(
        id: Int,
        name: String,
        nickname: String,
        description: String?,
        image: String?,
        preferredLanguage: String?,
        serversList: List<String>?,
        rank: Int?,
        country: String?,
        squadron: String?,
        squadronPatch: String?,
        medals: List<String>?
    ): String? {
        // Return the InsertStatement directly from dbQuery to avoid the nullable mutable var
        // that was captured via side-effect from outside the lambda.
        val statement = dbQuery {
            Profiles.insert { profiles ->
                profiles[Profiles.userId] = id
                profiles[Profiles.name] = name
                description?.let {
                    profiles[Profiles.description] = it
                }
                image?.let {
                    profiles[Profiles.image] = it
                }
            }
        }
        return rowToProfiles(statement.resultedValues?.get(0))
    }

    override suspend fun getProfile(userId: Int): String? {
        return dbQuery {
            // selectAll() fetches every column so future callers can read the full row
            // without a second round-trip; the earlier select(Profiles.userId) was a
            // partial projection that only returned the id column.
            Profiles.selectAll().where {
                Profiles.userId.eq(userId)
            }.toString()
        }
    }

    override suspend fun updateProfile(
        userId: Int,
        name: String?,
        nickname: String,
        description: String?,
        image: String?,
        location: String?,
        preferredLanguage: String?,
        serversList: List<String>?,
        rank: Int?,
        country: String?,
        squadron: String?,
        squadronPatch: String?,
        medals: List<String>?
    ): String? {
        return dbQuery {
            // The previous SELECT … FOR UPDATE before this UPDATE was a redundant database
            // round-trip.  The UPDATE statement itself acquires the necessary row lock, so
            // the extra SELECT only added latency without providing any concurrency benefit.
            Profiles.update({ Profiles.userId.eq(userId) }) { stmt ->
                name?.let { stmt[Profiles.name] = it }
                description?.let { stmt[Profiles.description] = it }
                image?.let { stmt[Profiles.image] = it }
                location?.let { stmt[Profiles.location] = it }
            }

            Profiles.selectAll().where {
                Profiles.userId.eq(userId)
            }.toString()
        }
    }

    private fun rowToProfiles(row: ResultRow?): String? {
        if (row == null) {
            return null
        }
        /*val geoLocation = getGeoLocationObjectFrom(row[Profiles.location])
        return Profile(
            id = row[Profiles.id],
            userId = row[Profiles.userId],
            name = row[Profiles.name],
            description = row[Profiles.description],
            image = row[Profiles.image],
            location = geoLocation
        )*/
        return ""
    }
/*
    private fun getGeoLocationObjectFrom(rowText: String): GeoLocation {
        val geoLocationText = rowText.split(',')
        return if (geoLocationText.size > 1) {
            GeoLocation(geoLocationText[0].toDouble(), geoLocationText[1].toDouble())
        } else {
            GeoLocation(0.0, 0.0)
        }
    }

 */
}
