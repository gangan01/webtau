package com.twosigma.webtau.data.table;

import com.twosigma.webtau.utils.JsonUtils;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

/**
 * Represents a set of rows with named columns to be used as part of test input preparation and/or test output validation
 */
public class TableData implements Iterable<Record> {
    private final List<Record> rows;
    private final Header header;

    public TableData(List<String> columnNames) {
        this(new Header(columnNames.stream()));
    }

    public TableData(Stream<String> columnNames) {
        this(new Header(columnNames));
    }

    public TableData(Header header) {
        this.header = header;
        this.rows = new ArrayList<>();
    }

    public Header getHeader() {
        return header;
    }

    public boolean isEmpty() {
        return rows.isEmpty();
    }

    /**
     * combine with header to define TableData in pure Java
     * @param values row values combined in one vararg
     * @return instance of table data
     */
    public TableData values(Object... values) {
        int numberOfRows = values.length / header.size();
        int numberOfExtraValues = values.length % header.size();

        if (numberOfExtraValues != 0) {
            int startIdxOfExtraValues = numberOfRows * header.size();
            throw new IllegalArgumentException("unfinished row idx " + numberOfRows + ", header:  " + header + "\nvalues so far: " +
                    Arrays.stream(values).skip(startIdxOfExtraValues).map(Object::toString).
                            collect(joining(", ")));
        }

        int total = numberOfRows * header.size();
        for (int i = 0; i < total; i += header.size()) {
            addRow(Arrays.stream(values).skip(i).limit(header.size()));
        }

        return this;
    }

    public Record row(int rowIdx) {
        validateRowIdx(rowIdx);
        return rows.get(rowIdx);
    }

    public void addRow(List<Object> values) {
        addRow(values.stream());
    }

    public void addRow(Record record) {
        if (header != record.getHeader()) {
            throw new RuntimeException("incompatible headers. current getHeader: " + header + ", new record one: " + record.getHeader());
        }
        addRow(record.values());
    }

    public void addRow(Stream<Object> values) {
        Record record = new Record(header, values);
        rows.add(record);
    }

    public TableData map(TableDataCellFunction mapper) {
        TableData mapped = new TableData(header);

        int rowIdx = 0;
        for (Record originalRow : rows) {
            mapped.addRow(mapRow(rowIdx, originalRow, mapper));
            rowIdx++;
        }

        return mapped;
    }

    @SuppressWarnings("unchecked")
    public <T, R> Stream<R> mapColumn(String columnName, Function<T, R> mapper) {
        int idx = header.columnIdxByName(columnName);
        return rows.stream().map(r -> mapper.apply(r.get(idx)));
    }

    @SuppressWarnings("unchecked")
    private <T, R> Stream<Object> mapRow(int rowIdx, Record originalRow, TableDataCellFunction mapper) {
        return header.getColumnIdxStream().mapToObj(idx -> mapper.apply(rowIdx, idx, header.columnNameByIdx(idx), originalRow.get(idx)));
    }

    public Stream<Record> rowsStream() {
        return rows.stream();
    }

    public List<Map<String, ?>> toListOfMaps() {
        return rows.stream().map(Record::toMap).collect(toList());
    }

    public String toJson() {
        return JsonUtils.serializePrettyPrint(toListOfMaps());
    }

    @Override
    public Iterator<Record> iterator() {
        return rows.iterator();
    }

    public int numberOfRows() {
        return rows.size();
    }

    private void validateRowIdx(int rowIdx) {
        if (rowIdx < 0 || rowIdx >= numberOfRows())
            throw new IllegalArgumentException("rowIdx is out of range: [0, " + (numberOfRows() - 1) + "]");
    }
}