package radar.spring.auth.managementportal

import kotlinx.coroutines.runBlocking
import org.radarbase.auth.authorization.EntityDetails
import org.radarbase.auth.authorization.EntityRelationService
import org.radarbase.auth.authorization.MPAuthorizationOracle
import org.radarbase.auth.authorization.Permission
import org.radarbase.auth.token.RadarToken
import org.slf4j.LoggerFactory
import radar.spring.auth.common.Authorization
import radar.spring.auth.common.PermissionOn

class ManagementPortalAuthorization() : Authorization<RadarToken> {
    private val DEFAULT_PROJECT = "main"
    private val relationService =
        object : EntityRelationService {
            override suspend fun findOrganizationOfProject(project: String): String? {
                // NOTE: This will default to the default "main" project for now since we are not using organizations
                // TODO: Implement organizations
                return DEFAULT_PROJECT
            }
        }
    val oracle: MPAuthorizationOracle = MPAuthorizationOracle(relationService)

    override fun hasPermission(
        token: RadarToken,
        permission: String,
        entity: String,
        permissionOn: PermissionOn?,
        project: String?,
        user: String?,
        source: String?
    ): Boolean {
        return runBlocking {
            val subject = user ?: token.subject
            val project = project ?: token.roles?.firstOrNull()?.referent

            val mpPermission =
                Permission.of(
                    Permission.Entity.valueOf(entity),
                    Permission.Operation.valueOf(permission)
                )
            when (permissionOn) {
                PermissionOn.PROJECT ->
                    checkPermissionOnProject(token, mpPermission, project, subject)
                PermissionOn.SUBJECT ->
                    checkPermissionOnSubject(token, mpPermission, project, subject)
                PermissionOn.SOURCE ->
                    checkPermissionOnSource(token, mpPermission, project, subject, source)
                PermissionOn.DEFAULT -> true
                else ->
                    oracle.hasPermission(
                        token,
                        mpPermission,
                        EntityDetails(project, subject, source)
                    )
            }
        }
    }

    override fun hasRole(
        token: RadarToken,
        project: String?,
        role: String?
    ): Boolean {
        if (role.isNullOrBlank()) {
            return true
        }
        if (project.isNullOrBlank()) {
            logger.warn("Project must be specified when checking a role.")
            return false
        }
        return token.roles.asSequence().filter { it.referent == project }.any {
            it.authority == role
        }
    }

    override fun hasScopes(token: RadarToken, scopes: Array<String>): Boolean {
        return token.scopes.containsAll(scopes.toList())
    }

    override fun hasAuthorities(token: RadarToken, authorities: Array<String>): Boolean {
        return token.roles.asIterable().map { it.authority }.containsAll(authorities.toList())
    }

    override fun hasAudiences(token: RadarToken, audiences: Array<String>): Boolean {
        return token.audience.containsAll(audiences.toList())
    }

    override fun hasGrantTypes(token: RadarToken, grantTypes: Array<String>): Boolean {
        if (grantTypes.isEmpty()) {
            return true
        }
        return grantTypes.contains(token.grantType)
    }

    private suspend fun checkPermissionOnProject(
        token: RadarToken,
        mpPermission: Permission,
        project: String?,
        subject: String?
    ): Boolean {
        if (project.isNullOrBlank()) {
            logger.warn("The project must be specified when checking permissions on PROJECT.")
            return false
        }
        return oracle.hasPermission(
            token,
            mpPermission,
            EntityDetails(subject = subject, project = project)
        )
    }

    private suspend fun checkPermissionOnSubject(
        token: RadarToken,
        mpPermission: Permission,
        project: String?,
        subject: String?
    ): Boolean {
        if (project.isNullOrBlank() || subject.isNullOrBlank()) {
            logger.warn(
                "The project and subject must be specified when checking permissions on SUBJECT."
            )
            return false
        }
        return oracle.hasPermission(
            token,
            mpPermission,
            EntityDetails(subject = subject, project = project)
        )
    }

    private suspend fun checkPermissionOnSource(
        token: RadarToken,
        mpPermission: Permission,
        project: String?,
        user: String?,
        source: String?,
    ): Boolean {
        if (project.isNullOrBlank() || user.isNullOrBlank() || source.isNullOrBlank()) {
            logger.warn(
                "The project, subject and source must be specified when checking permissions on " +
                    "SOURCE."
            )
            return false
        }
        return oracle.hasPermission(
            token,
            mpPermission,
            EntityDetails(user = user, source = source)
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ManagementPortalAuthorization::class.java)
    }
}
