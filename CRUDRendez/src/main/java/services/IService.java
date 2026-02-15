// Source code is decompiled from a .class file using FernFlower decompiler (from Intellij IDEA).
package services;

import java.sql.SQLException;
import java.util.List;

public interface IService<T> {
    void add(T var1) throws SQLException;

    void update(T var1) throws SQLException;

    void delete(T var1) throws SQLException;

    List<T> getAll() throws SQLException;
}
