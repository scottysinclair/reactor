package scott.reactor.diagram;

public class Link {

    private String name;

    private LinkType type;

    private Node from;

    private Node to;

    public Link(String name, LinkType type, Node from, Node to) {
        this.name = name;
        this.type = type;
        this.from = from;
        this.to = to;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LinkType getType() {
        return type;
    }

    public Node getFrom() {
        return from;
    }

    public Node getTo() {
        return to;
    }

}
