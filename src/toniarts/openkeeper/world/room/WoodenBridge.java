package toniarts.openkeeper.world.room;

import com.jme3.asset.AssetManager;
import com.jme3.math.FastMath;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.BatchNode;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import java.awt.Point;
import toniarts.openkeeper.tools.convert.AssetsConverter;
import toniarts.openkeeper.world.MapLoader;
import static toniarts.openkeeper.world.MapLoader.TILE_WIDTH;
import static toniarts.openkeeper.world.MapLoader.loadAsset;

/**
 *
 * @author ArchDemon
 */
public class WoodenBridge extends RoomConstructor {

    public static Spatial construct(AssetManager assetManager, RoomInstance roomInstance) {
        Node n = new Node(roomInstance.getRoom().getName());
        String modelName = AssetsConverter.MODELS_FOLDER + "/" + roomInstance.getRoom().getCompleteResource().getName();
        Point start = roomInstance.getCoordinates().get(0);
        // Contruct the tiles
        for (Point p : roomInstance.getCoordinates()) {
            // Figure out which peace by seeing the neighbours
            boolean N = roomInstance.hasCoordinate(new Point(p.x, p.y + 1));
            boolean NE = roomInstance.hasCoordinate(new Point(p.x - 1, p.y + 1));
            boolean E = roomInstance.hasCoordinate(new Point(p.x - 1, p.y));
            boolean SE = roomInstance.hasCoordinate(new Point(p.x - 1, p.y - 1));
            boolean S = roomInstance.hasCoordinate(new Point(p.x, p.y - 1));
            boolean SW = roomInstance.hasCoordinate(new Point(p.x + 1, p.y - 1));
            boolean W = roomInstance.hasCoordinate(new Point(p.x + 1, p.y));
            boolean NW = roomInstance.hasCoordinate(new Point(p.x + 1, p.y + 1));
            // 2x2
            BatchNode model = new BatchNode();
            for (int i = 0; i < 2; i++) {
                for (int j = 0; j < 2; j++) {

                    int pieceNumber = 0;
                    float yAngle = 0;
                    // Determine the piece
                    if (i == 1 && j == 1) { // North west corner
                        if (!N && !W) {
                            pieceNumber = 1;
                            yAngle = -FastMath.HALF_PI;
                        } else if (N && W && !NW) {
                            pieceNumber = 2;
                            yAngle = FastMath.HALF_PI;
                        } else if (!W) {
                            yAngle = FastMath.HALF_PI;
                        }
                    } else if (i == 0 && j == 1) { // North east corner
                        if (!N && !E) {
                            pieceNumber = 1;
                            yAngle = FastMath.PI;
                        } else if (N && E && !NE) {
                            pieceNumber = 2;
                        } else if (!E || N) {
                            yAngle = FastMath.HALF_PI;
                        }
                    } else if (i == 1 && j == 0) { // South west corner
                        if (!S && !W) {
                            pieceNumber = 1;
                        } else if (S && W && !SW) {
                            pieceNumber = 2;
                            yAngle = FastMath.PI;
                        } else if (!W || S) {
                            yAngle = FastMath.HALF_PI;
                        }
                    } else if (i == 0 && j == 0) { // South east corner
                        if (!S && !E) {
                            pieceNumber = 1;
                            yAngle = FastMath.HALF_PI;
                        } else if (S && E && !SE) {
                            pieceNumber = 2;
                            yAngle = -FastMath.HALF_PI;
                        } else if (!E) {
                            yAngle = FastMath.HALF_PI;
                        }
                    }
                    // Load the piece
                    Spatial part = loadAsset(assetManager, modelName + pieceNumber + ".j3o", false);
                    resetAndMoveSpatial((Node) part, start, p);
                    if (yAngle != 0) {
                        part.rotate(0, yAngle, 0);
                    }
                    part.move(i * TILE_WIDTH / 2 - TILE_WIDTH / 4, 0, j * TILE_WIDTH / 2 - TILE_WIDTH / 4);

                    // Set the shadows
                    // TODO: optimize, set to individual pieces and see zExtend whether it casts or not
                    part.setShadowMode(RenderQueue.ShadowMode.CastAndReceive);
                    model.attachChild(part);
                }
            }
            model.batch();
            n.attachChild(model);
        }
        // Set the transform and scale to our scale and 0 the transform
        n.move(start.x * MapLoader.TILE_WIDTH - MapLoader.TILE_WIDTH / 2, -0.1f, start.y * MapLoader.TILE_HEIGHT - MapLoader.TILE_HEIGHT / 2);
        n.scale(MapLoader.TILE_WIDTH); // Squares anyway...

        return n;
    }
}