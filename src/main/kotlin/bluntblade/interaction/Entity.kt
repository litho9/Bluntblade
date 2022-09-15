package bluntblade.interaction

import bluntblade.GameEntity
import bluntblade.game.PlayerProperty
import bluntblade.game.Stat
import bluntblade.inventory.vector

class EntityClientGadget(
    val ownerId: Int,
    val peerId: Int,
    private val req: EvtCreateGadgetNotify
): GameEntity(req.entityId) {
    override fun toProto() : SceneEntityInfo {
        return sceneEntityInfo {
            entityId = req.entityId
            entityType = ProtEntityType.PROT_ENTITY_TYPE_GADGET
            motionInfo = motionInfo {
                pos = req.initPos
                rot = req.initEulerAngles
                speed = vector {}
            }
            animatorParaList.add(animatorParameterValueInfoPair {})
            entityClientData = entityClientData {}
            entityAuthorityInfo = entityAuthorityInfo {
                abilityInfo = abilitySyncStateInfo {}
                rendererChangedInfo = entityRendererChangedInfo {}
                aiInfo = sceneEntityAiInfo {
                    isAiOpen = true
                    bornPos = vector {}
                }
                bornPos = vector {}
            }
            lifeState = 1
            propList.add(propPair {
                type = PlayerProperty.PROP_LEVEL.id
                propValue = prop(PlayerProperty.PROP_LEVEL.id, 1)
            })
            gadget = sceneGadgetInfo {
                gadgetId = req.configId
                ownerEntityId = req.propOwnerEntityId
                isEnableInteract = true
                clientGadget = clientGadgetInfo {
                    campId = req.campId
                    campType = req.campType
                    ownerEntityId = req.ownerEntityId
                    targetEntityId = req.targetEntityId
                    asyncLoad = req.isAsyncLoad
                }
                propOwnerEntityId = req.propOwnerEntityId
                authorityPeerId = peerId
            }
        }
    }
}

class EntityMonster(
    id: Int,
    val fightProps: MutableMap<Stat, Float> = mutableMapOf(),
): GameEntity(id)