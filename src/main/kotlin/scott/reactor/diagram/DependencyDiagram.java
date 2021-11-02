package scott.reactor.diagram;

import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;


public class DependencyDiagram {

    private String baseUrl = "https://yuml.me/diagram/TYPE/class/";

    private Map<String, Node> nodes = new HashMap<>();
    private Map<String, Link> linkKeys = new HashMap<>();

    public Node getOrCreate(String name) {
        name = name.replace('[', ' ').replace(']', ' ');
        Objects.requireNonNull(name);
        Node n = nodes.get(name);
        if (n == null) {
            nodes.put(name, n = new Node(name));
        }
        return n;
    }

    public Link link(String from, String to, String name, LinkType type) {
        String linkKey = from + to + name;
        Link l = linkKeys.get(linkKey);
        if (l == null) {
            Node nf = getOrCreate(from);
            Node nt = getOrCreate(to);
            l = new Link(name, type, nf, nt);
            nf.addLinkFrom(l);
            nt.addLinkTo(l);
            linkKeys.put(linkKey, l);
        }
        return l;
    }

    public void addNodeBackgroundColour(Pattern pattern, String colour) {
        for (Node node: nodes.values()) {
            if (pattern.matcher(node.getName()).find()) {
                node.setColour( colour );
            }
        }
    }


    public void filterOutLinks(Pattern fromNodePattern, Pattern linkNamePattern) {
        for (Node node: nodes.values()) {
            if (!fromNodePattern.matcher(node.getName()).find()) {
                continue;
            }
            for (Link link: new ArrayList<>(node.getLinksFrom())) {
                if (!linkNamePattern.matcher(link.getName()).find()) {
                    continue;
                }
                node.getLinksFrom().remove(link);
                link.getTo().getLinksTo().remove(link);
            }
        }
    }

    public void removeLinkText(Pattern fromNodePattern, Pattern linkNamePattern) {
        for (Node node: nodes.values()) {
            if (!fromNodePattern.matcher(node.getName()).find()) {
                continue;
            }
            for (Link link: new ArrayList<>(node.getLinksFrom())) {
                if (!linkNamePattern.matcher(link.getName()).find()) {
                    continue;
                }
                link.setName("");
            }
        }
    }

    public void generate(File file) throws IOException {
        generatePlain(file);
    }

    public void generateScruffy(File file) throws IOException {
        generate(file, "scruffy");
    }

    public void generatePlain(File file) throws IOException {
        generate(file, "plain");
    }


    private void generate(File file, String lookAndFeel) throws IOException {
        try ( InputStream response = requestYuml( buildYumlMessage(), lookAndFeel ); ) {
            writeToFile(response, file);
        }
    }

    private String buildYumlMessage() {
        StringBuilder yuml = new StringBuilder();
        for (Node node: nodes.values()) {
            for (Link link: node.getLinksFrom()) {
                renderNode(node, yuml);
                renderLink(link, yuml);
                renderNode(link.getTo(), yuml);
                yuml.append(", ");
            }
        }
        return yuml.toString();
    }

    private void renderLink(Link link, StringBuilder sb) {
        if (link.getName() != null) {
            sb.append(link.getName());
        }
        sb.append('-');
        if (link.getType() == LinkType.DEPENDENCY_DASHED) {
            sb.append(".-");
        }
        sb.append('>');
    }

    private void renderNode(Node node, StringBuilder sb) {
        sb.append('[');
        sb.append(node.getName());
        if (node.getColour() != null) {
            sb.append("{bg:");
            sb.append( node.getColour() );
            sb.append('}');
        }
        sb.append(']');
    }

    private InputStream requestYuml(Object buildYumlMessage, String lookAndFeel) throws IOException {
        URL url = new URL(baseUrl.replace("TYPE", lookAndFeel) + buildYumlMessage());
        return url.openStream();
    }

    private void writeToFile(InputStream response, File file) throws IOException {
        try (OutputStream out = new BufferedOutputStream(new FileOutputStream(file));) {
            int len;
            byte buf[] = new byte[1024 * 4];
            while((len = response.read(buf)) >= 0) {
                out.write(buf, 0, len);
                out.flush();
            }
        }
    }


    public String toYumlString() {
        return buildYumlMessage();
    }
}
