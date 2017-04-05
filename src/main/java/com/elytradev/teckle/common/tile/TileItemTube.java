package com.elytradev.teckle.common.tile;

import com.elytradev.teckle.common.TeckleObjects;
import com.elytradev.teckle.common.tile.base.TileNetworkMember;
import com.elytradev.teckle.common.worldnetwork.WorldNetwork;
import com.elytradev.teckle.common.worldnetwork.WorldNetworkNode;
import com.elytradev.teckle.common.worldnetwork.item.ItemNetworkEndpoint;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ITickable;

import java.util.List;

public class TileItemTube extends TileNetworkMember implements ITickable {

    @Override
    public void updateContainingBlockInfo() {
        super.updateContainingBlockInfo();
    }

    @Override
    public void update() {
    }

    @Override
    public void onLoad(){
        if (world == null || world.isRemote || getNode() == null || getNode().network == null)
            return;

        List<TileEntity> neighbourNodes = TeckleObjects.blockItemTube.getPotentialNeighbourNodes(world, pos, getNode().network, false);
        for (TileEntity neighbourNode : neighbourNodes) {
            if (neighbourNode instanceof TileNetworkMember) {
                if (!getNode().network.isNodePresent(neighbourNode.getPos())) {
                    getNode().network.registerNode(((TileNetworkMember) neighbourNode).getNode(getNode().network));
                    ((TileNetworkMember) neighbourNode).setNode(getNode().network.getNodeFromPosition(neighbourNode.getPos()));
                }
            } else {
                if (!getNode().network.isNodePresent(neighbourNode.getPos())) {
                    getNode().network.registerNode(new ItemNetworkEndpoint(getNode().network, neighbourNode.getPos()));
                }
            }
        }
    }

    @Override
    public WorldNetworkNode getNode(WorldNetwork network) {
        return new WorldNetworkNode(network, pos);
    }
}
