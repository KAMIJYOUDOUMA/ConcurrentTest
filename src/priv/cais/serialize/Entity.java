package priv.cais.serialize;

import java.io.Serializable;

/**
 * @auther CaiS
 */
public class Entity implements Serializable {
    private static final long serialVersionUID = -6414123022921547829L;

    private int id;

    private String name;

    private transient int tid;

    private transient String tname;

    public int getId() {
        return id;
    }

    public Entity setId(int id) {
        this.id = id;
        return this;
    }

    public String getName() {
        return name;
    }

    public Entity setName(String name) {
        this.name = name;
        return this;
    }

    public int getTid() {
        return tid;
    }

    public Entity setTid(int tid) {
        this.tid = tid;
        return this;
    }

    public String getTname() {
        return tname;
    }

    public Entity setTname(String tname) {
        this.tname = tname;
        return this;
    }

    @Override
    public String toString(){
        return "[id:" +id +",name:" + name + ",tid:" +tid +",tname:" + tname + "]" ;

    }
}
