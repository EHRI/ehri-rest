package eu.ehri.extension.utils;

import com.google.common.collect.Lists;

import java.util.List;

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

    public static Table of(List<List<String>> data) {
        return of(data, null);
    }

    public boolean hasHeaders() {
        return headers != null && !headers.isEmpty();
    }

    public List<List<String>> data() {
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
}
