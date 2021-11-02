package scott.reactor.diagram;

import java.util.LinkedHashSet;
import java.util.Set;

public class Node {

    private String name;

    private Set<Link> linksFrom = new LinkedHashSet<>();

    private Set<Link> linksTo= new LinkedHashSet<>();

    private String colour;

    public Node(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public Set<Link> getLinksFrom() {
        return linksFrom;
    }

    public Set<Link> getLinksTo() {
        return linksTo;
    }

    void addLinkFrom(Link link) {
        linksFrom.add(link);
    }

    void addLinkTo(Link link) {
        linksTo.add(link);
    }

    public String getColour() {
        return colour;
    }

    public void setColour(String colour) {
        this.colour = colour;
    }

}
