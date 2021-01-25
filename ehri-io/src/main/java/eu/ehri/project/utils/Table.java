package eu.ehri.project.utils;

import com.google.common.base.Objects;
import com.google.common.collect.Lists;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class Table {
    private final List<List<String>> data;
    private final List<String> headers;

    private Table(List<List<String>> data, List<String> headers) {
        this.data = data;
        this.headers = headers;
    }

    public static Table of(List<List<String>> data, List<String> headers) {
        return new Table(data, headers);
    }

    public static Table column(List<String> column1) {
        return new Table(column1.stream().map(Lists::newArrayList).collect(Collectors.toList()), null);
    }

    public static Table of(List<List<String>> data) {
        return of(data, null);
    }

    public boolean hasHeaders() {
        return headers != null && !headers.isEmpty();
    }

    public List<List<String>> rows() {
        return data;
    }

    public List<String> headers() {
        return headers;
    }

    @Override
    public String toString() {
        return data.toString();
    }

    public boolean contains(String... values) {
        return data.contains(Lists.newArrayList(values));
    }

    public List<String> column(int num) {
        if (data.isEmpty()) {
            return Collections.emptyList();
        } else {
            if (data.get(0).size() < num + 1) {
                throw new IllegalArgumentException("Column " + num + " does not exist");
            }
            return data
                    .stream()
                    .map(row -> row.size() < num + 1 ? "" : row.get(num))
                    .collect(Collectors.toList());
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Table table = (Table) o;
        return Objects.equal(data, table.data) &&
                Objects.equal(headers, table.headers);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(data, headers);
    }
}
