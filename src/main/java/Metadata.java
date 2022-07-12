import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class Metadata {

    public String filename;
    public String slices;
    public String box;

    public List<Integer> getSliceList() {

        List<String> items = Arrays.asList(this.slices.split("\\s*,\\s*"));
        try {
            return items.stream().map(i -> Integer.parseInt(i)).collect(Collectors.toList());
        } catch (Exception e) {
            return new ArrayList<>();
        }

    }

    public List<Integer> getBoxList() {

        List<String> items = Arrays.asList(this.box.split("\\s*,\\s*"));
        try {
            return items.stream().map(i -> Integer.parseInt(i)).collect(Collectors.toList());
        } catch (Exception e) {
            return new ArrayList<>();
        }

    }

}
