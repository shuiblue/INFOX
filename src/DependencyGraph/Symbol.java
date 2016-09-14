package DependencyGraph;

/**
 * Created by shuruiz on 8/29/16.
 */
public class Symbol {
    public String getType() {
        return type;
    }

    String type;
    String name;
    String tag;
    String location; //fileName+"-"+location
    int scope;

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    String alias;

    Symbol(String name, String type, String location, String tag, int scope) {
        this.name = name;
        this.type = type;
        this.location = location;
        this.tag = tag;
        this.scope = scope;
        this.alias = "";
    }

    Symbol(String name, String type, String location, String tag, int scope, String alias) {
        this.name = name;
        this.type = type;
        this.location = location;
        this.tag = tag;
        this.scope = scope;
        this.alias = alias;
    }


    public String getLocation() {
        return location;
    }

    public int getScope() {
        return scope;
    }


    public String getName() {
        return name;
    }

    public String getTag() {
        return tag;
    }
}
