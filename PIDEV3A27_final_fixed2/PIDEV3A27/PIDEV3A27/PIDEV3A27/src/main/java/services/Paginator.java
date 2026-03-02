package services;

public class Paginator {
    private int page = 1;
    //Nombre d’éléments par page.par defaut
    private int pageSize = 6;
    //Nombre total d’éléments dans la base de donnée
    private int totalItems = 0;

    //Getters
    public int getPage() { return page; }
    public int getPageSize() { return pageSize; }
    public int getTotalItems() { return totalItems; }

    //Permet de changer le nombre d’éléments par page.
    public void setPageSize(int pageSize) {
        this.pageSize = Math.max(1, pageSize);
        this.page = 1;
    }

    //Met à jour le nombre total d’éléments.
    public void setTotalItems(int totalItems) {
        //Empêche un total négatif.
        this.totalItems = Math.max(0, totalItems);
        //Calcule le nombre total de pages.
        int tp = getTotalPages();
        if (tp == 0) page = 1;
        else if (page > tp) page = tp;
        else if (page < 1) page = 1;
    }
//Calcule combien de pages au total.
    public int getTotalPages() {
        if (totalItems == 0) return 0;
        //20 éléments
        //6 par page
        //20 / 6 = 3.33 ,4 pages
        return (int) Math.ceil(totalItems / (double) pageSize);
    }
//Peut aller en arrière si page > 1.
    public boolean canPrev() { return page > 1; }
    //Peut aller en avant si page < total pages.
    public boolean canNext() { return page < getTotalPages(); }
//Passe à la page suivante si possible.
    public void next() { if (canNext()) page++; }
    public void prev() { if (canPrev()) page--; }
    public void first() { page = 1; }

    // Permet de forcer une page spécifique.
    public void setPage(int page) {
        //Empêche page < 1
        int p = Math.max(1, page);
        //Nombre total de pages.
        int tp = getTotalPages();
        //Si page demandée > total → on met dernière page.
        if (tp > 0 && p > tp) p = tp;
        this.page = p;
    }

    // ✅ AJOUT 2 : utilisé par tes requêtes SQL LIMIT/OFFSET
    public int getOffset() {
        return Math.max(0, (page - 1) * pageSize);
    }

    //Génère un texte pour l’interface.
    public String label() {
        int tp = getTotalPages();
        if (tp == 0) return "Page 0/0 • Total: 0";
        return "Page " + page + "/" + tp + " • Total: " + totalItems;
    }
}