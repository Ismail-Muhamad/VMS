
public final class ComboItem {

    public final int id;
    public final String label;

    public ComboItem(int id, String label) {
        this.id = id;
        this.label = label;
    }

    @Override
    public String toString() {
        return label;
    }
}
