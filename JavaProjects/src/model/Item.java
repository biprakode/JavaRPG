package model;

public abstract class Item {
    private String name;
    private String desc;
    private ItemType itemtype;

    Item(String name, String desc, ItemType itemtype) {
        this.name = name;
        this.desc = desc;
        this.itemtype = itemtype;
    }

    abstract void use(Player player);

    public String getName() {
        return name;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public ItemType getItemtype() {
        return itemtype;
    }

    public void setItemtype(ItemType itemtype) {
        this.itemtype = itemtype;
    }
}
