package ru.astakhovmd.Arena_plugin;

import com.google.common.annotations.Beta;
import net.minecraft.server.v1_16_R3.Position;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.json.simple.JSONObject;

public class Region {
    Position A,B;
    World world;
    RegionType type = RegionType.CYLINDER;
    double R;
    Region (RegionType t, Location a, Location b) throws NotSameWorld, NullPointerException, NoRegionTypeSet {
        if (a.getWorld() != b.getWorld()) throw new NotSameWorld();
        if (t==null) throw new NoRegionTypeSet();
        this.world = a.getWorld();
        switch (type) {
            case CUBOID:
                A = new Position(Double.min(a.getX(), b.getX()), Double.min(a.getY(), b.getY()), Double.min(a.getZ(), b.getZ()));
                B = new Position(Double.max(a.getX(), b.getX()), Double.max(a.getY(), b.getY()), Double.max(a.getZ(), b.getZ()));
                break;
            case SPHERE:
                A = new Position(a.getX(), Double.min(a.getY(), b.getY()), a.getZ());//center bottom

                B = new Position(b.getX(), Double.max(a.getY(), b.getY()), b.getZ());//top edge

                R = Math.sqrt(Math.pow(A.getX() - B.getX(), 2) + Math.pow(A.getZ() - B.getZ(), 2) + Math.pow(A.getY() - B.getY(), 2));
                break;
            case CYLINDER:
                A = new Position(a.getX(), Double.min(a.getY(), b.getY()), a.getZ());//center bottom

                B = new Position(b.getX(), Double.max(a.getY(), b.getY()), b.getZ());//top edge

                R = Math.sqrt(Math.pow(A.getX() - B.getX(), 2) + Math.pow(A.getZ() - B.getZ(), 2));
                break;
        }
        type = t;
    }

    Region (JSONObject obj) {
        if (obj==null) throw new NullPointerException();
        world = Bukkit.getWorld((String)obj.get("world"));
        JSONObject posA = (JSONObject) obj.get("A");
        A = new Position((double)posA.get("X"),(double)posA.get("Y"),(double)posA.get("Z"));
        JSONObject posB = (JSONObject) obj.get("B");
        B = new Position((double)posB.get("X"),(double)posB.get("Y"),(double)posB.get("Z"));

        if (type == RegionType.CYLINDER){
            R = Math.sqrt(Math.pow(A.getX() - B.getX(), 2)+Math.pow(A.getZ() - B.getZ(), 2));
        }
    }
    public JSONObject toJson(){
        JSONObject region = new JSONObject();
        region.put("world",world.getName());
        region.put("type", type.name());
        JSONObject posA = new JSONObject();
        posA.put("X", A.getX());
        posA.put("Y", A.getY());
        posA.put("Z", A.getZ());
        region.put("A",posA);
        JSONObject posB = new JSONObject();
        posB.put("X", B.getX());
        posB.put("Y", B.getY());
        posB.put("Z", B.getZ());
        region.put("B",posB);
        return region;
    }

    public boolean contains(Location pos){
        if (pos.getWorld() != world) return false;
        if (type == RegionType.CUBOID){
            if (A.getX() > pos.getX()) return false;
            if (pos.getX() > B.getX()) return false;

            if (A.getZ() > pos.getZ()) return false;
            if (pos.getZ() > B.getZ()) return false;
            return true;
        }else if (type == RegionType.CYLINDER){
            if (A.getY() > pos.getY()) return false;
            if (pos.getY() > B.getY()) return false;
            if (R < Math.sqrt(Math.pow(A.getX() - pos.getX(), 2)+Math.pow(A.getZ() - pos.getZ(), 2))) return false;
            return true;
        }
        return true;
    }

    @Beta
    public Location center(){
        return new Location(world, Double.sum(A.getX(), B.getX())/2,Double.sum(A.getY(), B.getY())/2,Double.sum(A.getZ(), B.getZ())/2);
    }

    /*public void setType(RegionType val) {
        this.type = val;
        if (type == RegionType.CYLINDER){
            R = Math.sqrt(Math.pow(A.getX() - B.getX(), 2)+Math.pow(A.getZ() - B.getZ(), 2));
        }
    }*/

    public Location nearest(Location pos, double multiplier){

        double x,y,z;
        if (type == RegionType.CYLINDER) {

            double diffX = pos.getX() - A.getX();
            double diffZ = pos.getZ() - A.getZ();
            double angle = Math.atan2(diffZ, diffX);

            x = A.getX() + R * multiplier * Math.cos(angle);
            z = A.getZ() + R * multiplier * Math.sin(angle);

            y = Double.max(A.getY() * multiplier,pos.getY());
            y = Double.min(B.getY() * multiplier,y);

        } /*if (type == Cuboid)*/
        else {
            x = Double.max(A.getX()*multiplier,pos.getX());
            x = Double.min(B.getX()*multiplier,x);

            y = Double.max(A.getY()*multiplier,pos.getY());
            y = Double.min(B.getY()*multiplier,y);

            z = Double.max(A.getZ()*multiplier,pos.getZ());
            z = Double.min(B.getZ()*multiplier,z);
        }
        return new Location(world,x,y,z, pos.getYaw(), pos.getPitch());
    }
}
