package radar.spring.auth.managementportal

import org.radarbase.auth.authorization.Permission
import org.radarbase.auth.token.RadarToken
import org.slf4j.LoggerFactory
import radar.spring.auth.common.Authorization
import radar.spring.auth.common.PermissionOn

class ManagementPortalAuthorization : Authorization<RadarToken> {

    override fun hasPermission(
        token: RadarToken,
        permission: String,
        entity: String,
        permissionOn: PermissionOn?,
        project: String?,
        user: String?,
        source: String?
    ): Boolean {
        val mpPermission = Permission(
            Permission.Entity.valueOf(entity), Permission.Operation.valueOf(permission)
        )
        return when (permissionOn) {
            PermissionOn.PROJECT -> checkPermissionOnProject(token, mpPermission, project)
            PermissionOn.SUBJECT -> checkPermissionOnSubject(token, mpPermission, project, user)
            PermissionOn.SOURCE -> checkPermissionOnSource(
                token,
                mpPermission,
                project,
                user,
                source
            )
            else -> token.hasPermission(mpPermission)
        }
    }

    override fun hasRole(token: RadarToken, project: String?, role: String?): Boolean {
        if (role.isNullOrBlank()) {
            return true
        }
        if (project.isNullOrBlank()) {
            logger.warn("Project must be specified when checking a role.")
            return false
        }
        return token.roles.getOrDefault(project, emptyList()).contains(role)
    }

    override fun hasScopes(token: RadarToken, scopes: Array<String>): Boolean {
        return token.scopes.containsAll(scopes.toList())
    }

    override fun hasAuthorities(token: RadarToken, authorities: Array<String>): Boolean {
        return token.authorities.containsAll(authorities.toList())
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


    private fun checkPermissionOnProject(
        token: RadarToken,
        mpPermission: Permission,
        project: String?
    ): Boolean {
        if (project.isNullOrBlank()) {
            logger.warn(
                "The project must be specified when checking " +
                        "permissions on PROJECT."
            )
            return false
        }
        return token.hasPermissionOnProject(mpPermission, project)
    }

    private fun checkPermissionOnSubject(
        token: RadarToken,
        mpPermission: Permission,
        project: String?,
        user: String?
    ): Boolean {
        if (project.isNullOrBlank() || user.isNullOrBlank()) {
            logger.warn(
                "The project and subject must be specified when checking " +
                        "permissions on SUBJECT."
            )
            return false
        }
        return token.hasPermissionOnSubject(mpPermission, project, user)
    }

    private fun checkPermissionOnSource(
        token: RadarToken, mpPermission: Permission, project:
        String?,
        user: String?,
        source: String?
    ): Boolean {
        if (project.isNullOrBlank() || user.isNullOrBlank() || source.isNullOrBlank()) {
            logger.warn(
                "The project, subject and source must be specified when checking " +
                        "permissions on SOURCE."
            )
            return false
        }
        return token.hasPermissionOnSource(mpPermission, project, user, source)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ManagementPortalAuthorization::class.java)
    }
}
