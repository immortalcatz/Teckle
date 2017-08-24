package com.elytradev.teckle.common.tile.retriever;

import com.elytradev.teckle.api.IWorldNetwork;
import com.elytradev.teckle.common.TeckleObjects;
import com.elytradev.teckle.common.block.BlockRetriever;
import com.elytradev.teckle.common.tile.sortingmachine.SortingMachineEndpoint;
import com.elytradev.teckle.common.worldnetwork.common.WorldNetworkTraveller;
import com.elytradev.teckle.common.worldnetwork.common.node.WorldNetworkNode;
import com.elytradev.teckle.common.worldnetwork.common.pathing.EndpointData;
import com.elytradev.teckle.common.worldnetwork.common.pathing.PathNode;
import com.google.common.collect.TreeMultiset;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.items.CapabilityItemHandler;

import java.util.*;

public class NetworkTileRetrieverInput extends NetworkTileRetrieverBase {

    protected TreeMultiset<PathNode> sourceNodes = TreeMultiset.create(Comparator.comparingInt(o -> o.cost));

    public NetworkTileRetrieverInput(World world, BlockPos pos, EnumFacing face) {
        super(world, pos, face);
    }

    public NetworkTileRetrieverInput(TileRetriever retriever) {
        super(retriever.getWorld(), retriever.getPos(), retriever.getFacing().getOpposite());

        this.filterData = retriever.filterData;
        this.bufferData = retriever.bufferData;
        this.filterID = retriever.filterID;
        this.bufferID = retriever.bufferID;
    }

    @Override
    public WorldNetworkNode createNode(IWorldNetwork network, BlockPos pos) {
        return new SortingMachineEndpoint(network, pos, getCapabilityFace());
    }

    @Override
    public boolean canAcceptTraveller(WorldNetworkTraveller traveller, EnumFacing from) {
        return false;
    }

    @Override
    public EnumFacing getCapabilityFace() {
        if (getWorld() != null && getWorld().isBlockLoaded(getPos())) {
            IBlockState thisState = getWorld().getBlockState(getPos());
            if (Objects.equals(thisState.getBlock(), TeckleObjects.blockRetriever)) {
                setCapabilityFace(thisState.getValue(BlockRetriever.FACING).getOpposite());
            }
        }

        return super.getCapabilityFace();
    }

    @Override
    public boolean listenToNetworkChange() {
        return true;
    }

    @Override
    public void onNodeAdded(WorldNetworkNode addedNode) {
        // Only add if it's not already present, and has IO for transfer of items.
        if (addedNode.isEndpoint() && sourceNodes.stream().noneMatch(pN -> pN.realNode.equals(addedNode))) {
            IWorldNetwork network = this.getNode().getNetwork();
            List<PathNode> nodeStack = new ArrayList<>();
            List<BlockPos> iteratedPositions = new ArrayList<>();
            HashMap<BlockPos, HashMap<EnumFacing, EndpointData>> endpoints = new HashMap<>();

            nodeStack.add(new PathNode(null, this.getNode(), null));
            while (!nodeStack.isEmpty() && endpoints.size() < 6) {
                PathNode pathNode = nodeStack.remove(nodeStack.size() - 1);
                for (EnumFacing direction : EnumFacing.VALUES) {
                    BlockPos neighbourPos = pathNode.realNode.position.add(direction.getDirectionVec());
                    if (!network.isNodePresent(neighbourPos) || neighbourPos.equals(this.getNode().position) ||
                            iteratedPositions.contains(neighbourPos) ||
                            (endpoints.containsKey(neighbourPos) && endpoints.get(neighbourPos).containsKey(direction.getOpposite()))) {
                        continue;
                    }

                    WorldNetworkNode neighbourNode = network.getNode(neighbourPos, direction.getOpposite());
                    if (isValidSourceNode(neighbourPos, direction)) {
                        if (!endpoints.containsKey(neighbourPos)) {
                            endpoints.put(neighbourPos, new HashMap<>());
                        }
                        endpoints.get(neighbourPos).put(direction.getOpposite(),
                                new EndpointData(new PathNode(pathNode, neighbourNode, direction.getOpposite()),
                                        direction.getOpposite()));
                    } else {
                        if (neighbourNode.canConnectTo(direction.getOpposite())) {
                            nodeStack.add(new PathNode(pathNode, neighbourNode, direction.getOpposite()));
                            iteratedPositions.add(neighbourPos);
                        }
                    }

                    if (endpoints.size() < 6) {
                        break;
                    }
                }
            }

            for (Map.Entry<BlockPos, HashMap<EnumFacing, EndpointData>> entry : endpoints.entrySet()) {
                for (EndpointData endpointData : entry.getValue().values()) {
                    if (sourceNodes.stream().noneMatch(pathNode -> pathNode.realNode.position.equals(endpointData.pos)
                            && pathNode.from.equals(endpointData.node.from))) {
                        sourceNodes.add(endpointData.node);
                    }
                }
            }
        }
    }

    @Override
    public void onNodeRemoved(WorldNetworkNode removedNode) {
        // Remove the node if it's known to us.
        sourceNodes.removeIf(pN -> pN.realNode.equals(removedNode) || pN.realNode.position.equals(removedNode.position));
    }

    @Override
    public boolean isValidNetworkMember(IWorldNetwork network, EnumFacing side) {
        return Objects.equals(side, getCapabilityFace());
    }


    @Override
    public boolean canConnectTo(EnumFacing side) {
        return Objects.equals(side, getCapabilityFace());
    }

    private boolean isValidSourceNode(BlockPos position, EnumFacing direction) {
        direction = direction.getOpposite();

        TileEntity tileEntity = getWorld().getTileEntity(position);
        if (tileEntity != null && tileEntity.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, direction)) {
            return true;
        }

        return false;
    }

    public ItemStack acceptTraveller(WorldNetworkTraveller traveller, EnumFacing from) {
        //TODO: Implement for accepting when inline.
        return ItemStack.EMPTY;
    }
}