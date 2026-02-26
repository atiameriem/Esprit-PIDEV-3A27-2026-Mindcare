package services;

public class Paginator {
    private int page = 1;
    private int pageSize = 6;
    private int totalItems = 0;

    public int getPage() { return page; }
    public int getPageSize() { return pageSize; }
    public int getTotalItems() { return totalItems; }

    public void setPageSize(int pageSize) {
        this.pageSize = Math.max(1, pageSize);
        this.page = 1;
    }

    public void setTotalItems(int totalItems) {
        this.totalItems = Math.max(0, totalItems);
        int tp = getTotalPages();
        if (tp == 0) page = 1;
        else if (page > tp) page = tp;
        else if (page < 1) page = 1;
    }

    public int getTotalPages() {
        if (totalItems == 0) return 0;
        return (int) Math.ceil(totalItems / (double) pageSize);
    }

    public boolean canPrev() { return page > 1; }
    public boolean canNext() { return page < getTotalPages(); }

    public void next() { if (canNext()) page++; }
    public void prev() { if (canPrev()) page--; }
    public void first() { page = 1; }

    // ✅ AJOUT 1 : utilisé par tes controllers (setPage)
    public void setPage(int page) {
        int p = Math.max(1, page);
        int tp = getTotalPages();
        if (tp > 0 && p > tp) p = tp;
        this.page = p;
    }

    // ✅ AJOUT 2 : utilisé par tes requêtes SQL LIMIT/OFFSET
    public int getOffset() {
        return Math.max(0, (page - 1) * pageSize);
    }

    public String label() {
        int tp = getTotalPages();
        if (tp == 0) return "Page 0/0 • Total: 0";
        return "Page " + page + "/" + tp + " • Total: " + totalItems;
    }
}