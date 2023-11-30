import { get, orderBy } from "lodash";
import { convertArrayToObject } from "../configuration-tables/helper";

export class RestConfigTableRows {
  constructor(restData = null) {
    this._rows = undefined;
    if (restData) {
      this._rows = restData;
    }
  }

  get rows() {
    return this._rows;
  }

  set rows(val) {
    this._rows = val;
  }

  convertToTableRows(rows, columns) {
    const newArray = rows.map((row, rowIndex) => {
      let keys = {};
      columns.forEach((col, colIndex) => {
        const value = get(row, [col.dataIndex], "");
        keys = {
          ...keys,
          [col.id]: value
        };
      });
      return {
        id: row.id,
        index: rowIndex,
        ...keys
      };
    });

    this._rows = convertArrayToObject(newArray);
  }

  convertFromTableRows(schema) {
    const rows = Object.keys(this._rows).map((row, rowIndex) => {
      const id = get(this._rows, [row, "id"], row);
      const index = get(this._rows, [row, "index"], "");
      let newKeys = {};
      const columns = schema.columns;
      Object.keys(columns).forEach(col => {
        const key = get(columns, [col, "index"], "");
        const colId = get(columns, [col, "id"], "");
        const value = get(this._rows, [row, colId], "");
        newKeys = {
          ...newKeys,
          [`column_${key}`]: value
        };
      });
      return {
        id,
        index,
        ...newKeys
      };
    });

    return orderBy(rows, ["index"], ["asc"]);
  }

  json() {
    return {
      ...this._rows
    };
  }
}

export class RestConfigTableSchema {
  constructor(restData = null) {
    this._columns = undefined;
    if (restData) {
      this._columns = Object.keys(restData.columns).map(col => new RestConfigTableColumn(restData.columns[col]));
    }

    this.convertToTableSchema = this.convertToTableSchema.bind(this);
    this.json = this.json.bind(this);
  }

  get columns() {
    return this._columns;
  }

  set columns(value) {
    this._columns = value;
  }

  convertToTableSchema(columns) {
    const newArray = columns.map((rawCol, index) => {
      const newCol = new RestConfigTableColumn();
      newCol.convertToTableCol(rawCol);
      return newCol;
    });
    this._columns = convertArrayToObject(newArray);
  }

  convertFromTableSchema() {
    const columns = this._columns.map(restCol => restCol.convertFromTableCol());
    return orderBy(columns, ["index"], ["asc"]);
  }

  json() {
    return {
      columns: convertArrayToObject(Object.keys(this._columns).map(restCol => this._columns[restCol].json()))
    };
  }
}

export class RestConfigTableColumn {
  constructor(restData = null) {
    this._id = restData?.id || undefined;
    this._index = restData?.index || undefined;
    this._key = restData?.key || undefined;
    this._display_name = restData?.display_name || "";
    this._type = restData?.type || "string";
    this._options = restData?.options || undefined;
    this._required = restData?.required || false;
    this._default_value = restData?.default_value || "";
    this._readOnly = restData?.read_only || false;

    this.json = this.json.bind(this);
    this.convertToTableCol = this.convertToTableCol.bind(this);
    this.convertFromTableCol = this.convertFromTableCol.bind(this);
  }

  get id() {
    return this._id;
  }

  set id(value) {
    this._id = value;
  }

  get index() {
    return this._index;
  }

  set index(value) {
    this._index = value;
  }

  get display_name() {
    return this._display_name;
  }

  set display_name(value) {
    this._display_name = value;
  }

  get required() {
    return this._required;
  }

  set required(value) {
    this._required = value;
  }

  get type() {
    return this._type;
  }

  set type(value) {
    this._type = value;
  }

  get key() {
    return this._key;
  }

  set key(value) {
    this._key = value;
  }

  get options() {
    return this._options;
  }

  set options(value) {
    this._options = value;
  }

  get defaultValue() {
    return this._default_value;
  }

  set defaultValue(value) {
    this._default_value = value;
  }

  get readOnly() {
    return this._readOnly;
  }

  set readOnly(value) {
    this._readOnly = value;
  }

  json() {
    return {
      id: this._id,
      index: this._index,
      key: this._key,
      display_name: this._display_name,
      type: this._type,
      required: this._required,
      options: this._options,
      default_value: this._default_value,
      read_only: this._readOnly
    };
  }

  convertToTableCol(rawCol) {
    this._id = rawCol.id;
    this._index = parseInt(rawCol.dataIndex.split("_")[1]);
    this._key = rawCol.dataIndex;
    this._display_name = rawCol.title;
    this._type = rawCol.inputType;
    this._required = rawCol.required;
    this._options = rawCol.options || [];
    this._default_value = rawCol.defaultValue;
    this._readOnly = rawCol.readOnly;
  }

  convertFromTableCol() {
    return {
      id: this._id,
      dataIndex: `column_${this._index}`,
      index: this._index,
      title: this._display_name,
      inputType: this._type,
      required: this._required,
      options: this._options,
      editable: true,
      defaultValue: this._default_value,
      readOnly: this._readOnly,
      key: this._key
    };
  }
}

export class RestConfigTable {
  constructor(restData = null) {
    this._id = undefined;
    this._name = undefined;
    this._total_rows = 0;
    this._version = undefined;
    this._created_by = undefined;
    this._updated_by = undefined;
    this._created_at = undefined;
    this._updated_at = undefined;
    this._rows = undefined;
    this._schema = {};
    this._history = {};
    if (restData) {
      this._id = restData?.id;
      this._name = restData?.name;
      this._total_rows = restData?.total_rows;
      this._version = restData?.version;
      this._created_at = restData?.created_at;
      this._created_by = restData?.created_by;
      this._updated_at = restData?.updated_at;
      this._updated_by = restData?.updated_by;
      this._history = restData?.history;
      this._schema = new RestConfigTableSchema(restData?.schema); // TODO make a separate rest class out of it
      this._rows = new RestConfigTableRows(restData?.rows); // TODO make a separate rest class out of it
    }

    this.json = this.json.bind(this);
    this.convertToTableColAndRows = this.convertToTableColAndRows.bind(this);
    this.convertFromTable = this.convertFromTable.bind(this);
  }

  get id() {
    return this._id;
  }

  set id(value) {
    this._id = value;
  }

  get name() {
    return this._name;
  }

  set name(value) {
    this._name = value;
  }

  get total_rows() {
    return this._total_rows;
  }

  get version() {
    return this._version;
  }

  get history() {
    return this._history;
  }

  get rows() {
    return this._rows;
  }

  set rows(value) {
    this._rows = value;
  }

  get schema() {
    return this._schema;
  }

  set schema(value) {
    this._schema = value;
  }

  get created_at() {
    return this._created_at;
  }

  get created_by() {
    return this._created_by;
  }

  set created_by(value) {
    this._created_by = value;
  }

  get updated_at() {
    return this._updated_at;
  }

  get updated_by() {
    return this._updated_by;
  }

  set updated_by(value) {
    this._updated_by = value;
  }

  convertToTableColAndRows(rows, columns) {
    const restRows = new RestConfigTableRows();
    restRows.convertToTableRows(rows, columns);

    const restSchema = new RestConfigTableSchema();
    restSchema.convertToTableSchema(columns);

    this._rows = restRows;
    this._schema = restSchema;
  }

  convertFromTable() {
    return {
      id: this._id,
      name: this._name,
      total_rows: this._total_rows,
      version: this._version,
      created_by: this._created_by,
      updated_by: this._updated_by,
      created_at: this._created_at,
      updated_at: this._updated_at,
      rows: this._rows.convertFromTableRows(this._schema),
      columns: this._schema.convertFromTableSchema(),
      history: this._history
    };
  }

  json() {
    return {
      id: this._id,
      name: this._name,
      total_rows: this._total_rows,
      version: this._version,
      created_by: this._created_by,
      updated_by: this._updated_by,
      created_at: this._created_at,
      updated_at: this._updated_at,
      rows: this._rows.json(),
      schema: this._schema.json(),
      history: this._history
    };
  }
}
