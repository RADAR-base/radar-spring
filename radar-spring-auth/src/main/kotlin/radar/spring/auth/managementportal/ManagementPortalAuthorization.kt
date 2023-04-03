package radar.spring.auth.managementportal

import org.radarbase.auth.authorization.EntityDetails
import org.radarbase.auth.authorization.MPAuthorizationOracle
import org.radarbase.auth.authorization.Permission
import org.radarbase.auth.token.RadarToken
import org.slf4j.LoggerFactory
import radar.spring.auth.common.Authorization
import radar.spring.auth.common.PermissionOn

class ManagementPortalAuthorization : Authorization<RadarToken> {
    private val authorizationOracle = MPAuthorizationOracle(

    )

    override fun hasPermission(
        token: RadarToken,
        permission: Permission,
        entity: EntityDetails,
    ): Boolean {

        val mpPermission = Permission.of(
            Permission.Entity.valueOf(entity),
            Permission.Operation.valueOf(permission)
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
        return token.roles.asSequence()
            .filter { it.referent == project }
            .any { it.authority == role }
    }

    override fun hasScopes(token: RadarToken, scopes: Array<String>): Boolean {
        return token.scopes.containsAll(scopes.toList())
    }

    override fun hasAudiences(token: RadarToken, audiences: Array<String>): Boolean {
        return token.audience.containsAll(audiences.toList())
    }

    override fun hasGrantTypes(token: RadarToken, grantTypes: Array<String>): Boolean {
        return grantTypes.isEmpty() ||
            token.grantType in grantTypes
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ManagementPortalAuthorization::class.java)
    }
}
