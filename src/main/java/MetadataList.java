import java.util.Arrays;

public class MetadataList {

    public Metadata[] data;

    @Override
    public String toString() {

        String retStr = "";

        for (int i = 0; i < data.length; ++i) {

            retStr += String.format("%s:\t%s\t%s\n", data[i].filename, data[i].slices, data[i].box);

        }

        return retStr;

    }

}
