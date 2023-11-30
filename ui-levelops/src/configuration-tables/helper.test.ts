import moment from "moment";

import * as helperFunctions from "./helper";

describe("configuration-tables helper test suite", () => {
  const columns = [
    {
      id: 1,
      title: `Column 1`,
      dataIndex: 1,
      key: 1,
      inputType: "string",
      editable: true,
      readOnly: false
    },
    {
      id: 2,
      title: `Column 2`,
      dataIndex: 2,
      key: 2,
      inputType: "string",
      editable: true,
      readOnly: false
    },
    {
      id: 3,
      title: `Column 3`,
      dataIndex: 3,
      key: 3,
      inputType: "string",
      editable: true,
      readOnly: false
    }
  ];

  test("getNewData should insert new element at first position", () => {
    expect(helperFunctions.getNewData([2, 3, 4], 1, -1)).toStrictEqual([1, 2, 3, 4]);
  });

  test("getNewData should insert new element at nth position", () => {
    expect(helperFunctions.getNewData([2, 3, 4, 5, 6], 1, 3)).toStrictEqual([2, 3, 4, 5, 1, 6]);
    expect(helperFunctions.getNewData([2, 3, 4, 5, 6], 1, 4)).toStrictEqual([2, 3, 4, 5, 6, 1]);
  });

  test("getUpdatedColumns should insert new element at nth position", () => {
    const newEl = {
      id: 4,
      title: `Column 4`,
      dataIndex: 4,
      key: 4,
      inputType: "string",
      editable: true,
      readOnly: false
    };

    expect(helperFunctions.getNewData(columns, newEl, 1)).toStrictEqual([
      {
        dataIndex: 1,
        editable: true,
        id: 1,
        inputType: "string",
        key: 1,
        readOnly: false,
        title: "Column 1"
      },
      {
        dataIndex: 2,
        editable: true,
        id: 2,
        inputType: "string",
        key: 2,
        readOnly: false,
        title: "Column 2"
      },
      {
        dataIndex: 4,
        editable: true,
        id: 4,
        inputType: "string",
        key: 4,
        readOnly: false,
        title: "Column 4"
      },
      {
        dataIndex: 3,
        editable: true,
        id: 3,
        inputType: "string",
        key: 3,
        readOnly: false,
        title: "Column 3"
      }
    ]);
    expect(helperFunctions.getNewData(columns, newEl, 2)).toStrictEqual([
      {
        dataIndex: 1,
        editable: true,
        id: 1,
        inputType: "string",
        key: 1,
        readOnly: false,
        title: "Column 1"
      },
      {
        dataIndex: 2,
        editable: true,
        id: 2,
        inputType: "string",
        key: 2,
        readOnly: false,
        title: "Column 2"
      },
      {
        dataIndex: 3,
        editable: true,
        id: 3,
        inputType: "string",
        key: 3,
        readOnly: false,
        title: "Column 3"
      },
      {
        dataIndex: 4,
        editable: true,
        id: 4,
        inputType: "string",
        key: 4,
        readOnly: false,
        title: "Column 4"
      }
    ]);
  });

  test("getDefaultInputValue should return correct value", () => {
    expect(helperFunctions.getDefaultInputValue("string")).toEqual("text (DEFAULT)");
    expect(helperFunctions.getDefaultInputValue("boolean")).toEqual("false (DEFAULT)");
    expect(helperFunctions.getDefaultInputValue("date")).toEqual(`${moment().format("MM/DD/YYYY")} (DEFAULT)`);
    expect(helperFunctions.getDefaultInputValue("single-select")).toEqual("preset (DEFAULT)");
    expect(helperFunctions.getDefaultInputValue("xyz")).toEqual("");
  });

  test("getColData should return correct value", () => {
    expect(helperFunctions.getColData(columns, 1)).toStrictEqual({ dataIndex: 2, inputType: "string" });
  });

  test("validateCell should return correct value", () => {
    expect(helperFunctions.validateCell("string", 1)).toEqual(false);
    expect(helperFunctions.validateCell("string", "")).toEqual(false);
    expect(helperFunctions.validateCell("string", "qwerty")).toEqual(true);
    expect(helperFunctions.validateCell("boolean", 19238)).toEqual(false);
    expect(helperFunctions.validateCell("boolean", "True")).toEqual(true);
    expect(helperFunctions.validateCell("boolean", "False")).toEqual(true);
    expect(helperFunctions.validateCell("date", true)).toEqual(false);
    expect(helperFunctions.validateCell("date", "12/12/1222")).toEqual(true);
    expect(helperFunctions.validateCell("single-select", "12/12/1222")).toEqual(false);
    expect(helperFunctions.validateCell("single-select", 1)).toEqual(false);
    expect(helperFunctions.validateCell("single-select", "test")).toEqual(false);
    expect(helperFunctions.validateCell("single-select", "test", ["one", "two"])).toEqual(true);
    expect(helperFunctions.validateCell("test", "test")).toEqual(false);
  });

  test("defaultTableColumns to match snapshot", () => {
    expect(helperFunctions.defaultTableColumns).toMatchSnapshot();
  });

  test("defaultRowData to match snapshot", () => {
    expect(helperFunctions.defaultRowData).toMatchSnapshot();
  });

  test("convertArrayToObject should convert array into object", () => {
    expect(
      helperFunctions.convertArrayToObject([
        {
          id: "qwe",
          name: "test",
          email: "test@gmail.com"
        },
        {
          id: "qwerty",
          name: "qwerty",
          email: "qwerty@gmail.com"
        }
      ])
    ).toStrictEqual({
      qwe: {
        email: "test@gmail.com",
        id: "qwe",
        name: "test"
      },
      qwerty: {
        email: "qwerty@gmail.com",
        id: "qwerty",
        name: "qwerty"
      }
    });
  });

  test("convertFromTableSchema should return correct value", () => {
    expect(
      helperFunctions.convertFromTableSchema({
        id: "73a0f77c-72e5-4064-9186-edc3302462b5",
        name: "yueyreyru",
        schema: {
          columns: {
            "48e2c800-6172-11eb-8570-75e5c3540061": {
              index: 1,
              id: "48e2c800-6172-11eb-8570-75e5c3540061",
              key: "column_1",
              display_name: "Column 1",
              type: "boolean",
              options: []
            },
            "48e2c801-6172-11eb-8570-75e5c3540061": {
              index: 2,
              id: "48e2c801-6172-11eb-8570-75e5c3540061",
              key: "column_2",
              display_name: "Column 2",
              type: "boolean",
              options: []
            },
            "48e2c802-6172-11eb-8570-75e5c3540061": {
              index: 3,
              id: "48e2c802-6172-11eb-8570-75e5c3540061",
              key: "column_3",
              display_name: "Column 3",
              type: "date",
              options: []
            },
            "48e2c803-6172-11eb-8570-75e5c3540061": {
              index: 4,
              id: "48e2c803-6172-11eb-8570-75e5c3540061",
              key: "column_4",
              display_name: "Column 4",
              type: "single-select",
              options: ["preset"]
            }
          }
        },
        total_rows: 1,
        rows: {
          "48e2c804-6172-11eb-8570-75e5c3540061": {
            id: "48e2c804-6172-11eb-8570-75e5c3540061",
            index: 0,
            "48e2c803-6172-11eb-8570-75e5c3540061": "",
            "48e2c801-6172-11eb-8570-75e5c3540061": "",
            "48e2c802-6172-11eb-8570-75e5c3540061": "",
            "48e2c800-6172-11eb-8570-75e5c3540061": "False"
          }
        },
        version: "1",
        history: {
          "1": {
            version: "1",
            created_at: 1611843057335,
            user_id: "39"
          }
        },
        created_by: "39",
        created_at: 1611843057335
      })
    ).toStrictEqual({
      columns: [
        {
          dataIndex: "column_1",
          editable: true,
          id: "48e2c800-6172-11eb-8570-75e5c3540061",
          index: 1,
          inputType: "boolean",
          options: [],
          title: "Column 1",
          defaultValue: undefined,
          readOnly: undefined,
          required: undefined
        },
        {
          dataIndex: "column_2",
          editable: true,
          id: "48e2c801-6172-11eb-8570-75e5c3540061",
          index: 2,
          inputType: "boolean",
          options: [],
          title: "Column 2",
          defaultValue: undefined,
          readOnly: undefined,
          required: undefined
        },
        {
          dataIndex: "column_3",
          editable: true,
          id: "48e2c802-6172-11eb-8570-75e5c3540061",
          index: 3,
          inputType: "date",
          options: [],
          title: "Column 3",
          defaultValue: undefined,
          readOnly: undefined,
          required: undefined
        },
        {
          dataIndex: "column_4",
          editable: true,
          id: "48e2c803-6172-11eb-8570-75e5c3540061",
          index: 4,
          inputType: "single-select",
          options: ["preset"],
          title: "Column 4",
          defaultValue: undefined,
          readOnly: undefined,
          required: undefined
        }
      ],
      created_at: 1611843057335,
      created_by: "39",
      history: {
        "1": {
          created_at: 1611843057335,
          user_id: "39",
          version: "1"
        }
      },
      id: "73a0f77c-72e5-4064-9186-edc3302462b5",
      name: "yueyreyru",
      rows: [
        {
          column_1: "False",
          column_2: "",
          column_3: "",
          column_4: "",
          id: "48e2c804-6172-11eb-8570-75e5c3540061",
          index: 0
        }
      ],
      total_rows: 1,
      version: "1",
      updated_at: undefined,
      updated_by: undefined
    });
  });

  test("checkForColumnName should return correct value", () => {
    const columns = [
      {
        id: "0",
        dataIndex: "column_1",
        index: 1,
        title: "Column for distinct test",
        inputType: "string",
        required: true,
        options: [],
        editable: true,
        defaultValue: "test",
        readOnly: false
      },
      {
        id: "1",
        dataIndex: "column_2",
        index: 2,
        title: "Column for update test",
        inputType: "string",
        required: false,
        options: [],
        editable: true,
        defaultValue: "",
        readOnly: false
      },
      {
        id: "2",
        dataIndex: "column_3",
        index: 3,
        title: "A",
        inputType: "string",
        required: false,
        options: [],
        editable: true,
        defaultValue: "test",
        readOnly: true
      },
      {
        id: "3",
        dataIndex: "column_4",
        index: 4,
        title: "Column 4",
        inputType: "string",
        required: false,
        options: [],
        editable: true,
        defaultValue: "",
        readOnly: false
      },
      {
        id: "4",
        dataIndex: "column_5",
        index: 5,
        title: "B",
        inputType: "string",
        required: false,
        options: [],
        editable: true,
        defaultValue: "",
        readOnly: false
      }
    ];
    expect(helperFunctions.checkForColumnName(columns, "column_4", "column for update test")).toStrictEqual(true);

    expect(helperFunctions.checkForColumnName(columns, "column_2", "column for update test")).toStrictEqual(false);
  });

  test("deleteEmptyRows should delete all empty rows", () => {
    const mockData = {
      id: "46eeb803-e450-4450-a2c0-05d8e754f475",
      name: "test for import table",
      total_rows: 6,
      version: "5",
      created_by: "21",
      updated_by: "39",
      created_at: 1610631878950,
      updated_at: 1612425095545,
      rows: [
        {
          id: "0",
          index: 0,
          column_1: "a",
          column_2: "2020-09-30T23:45:31.943439Z",
          column_3: "1",
          column_4: "",
          column_5: "green"
        },
        {
          id: "1",
          index: 1,
          column_1: "b",
          column_2: "09/06/2020",
          column_3: "2",
          column_4: "",
          column_5: "blue"
        },
        {
          id: "2",
          index: 2,
          column_1: "a",
          column_2: "",
          column_3: "3",
          column_4: "",
          column_5: "green"
        },
        {
          id: "3",
          index: 3,
          column_1: "c, d",
          column_2: "",
          column_3: "4",
          column_4: "",
          column_5: "blue"
        },
        {
          id: "4",
          index: 4,
          column_1: "b",
          column_2: "Wed Dec 23 2020 08:50:22 GMT+0000 (GMT)",
          column_3: "5",
          column_4: "",
          column_5: "green"
        },
        {
          id: "a24cde30-566e-11eb-8d7a-69434d69c3a2",
          index: 5,
          column_1: "test",
          column_2: "",
          column_3: "test",
          column_4: "",
          column_5: ""
        }
      ],
      columns: [
        {
          id: "0",
          dataIndex: "column_1",
          index: 1,
          title: "Column for distinct test",
          inputType: "string",
          required: true,
          options: [],
          editable: true,
          defaultValue: "test",
          readOnly: false
        },
        {
          id: "1",
          dataIndex: "column_2",
          index: 2,
          title: "Column for update test",
          inputType: "string",
          required: false,
          options: [],
          editable: true,
          defaultValue: "",
          readOnly: false
        },
        {
          id: "2",
          dataIndex: "column_3",
          index: 3,
          title: "A",
          inputType: "string",
          required: false,
          options: [],
          editable: true,
          defaultValue: "test",
          readOnly: true
        },
        {
          id: "3",
          dataIndex: "column_4",
          index: 4,
          title: "Column 4",
          inputType: "string",
          required: false,
          options: [],
          editable: true,
          defaultValue: "",
          readOnly: false
        },
        {
          id: "4",
          dataIndex: "column_5",
          index: 5,
          title: "B",
          inputType: "string",
          required: false,
          options: [],
          editable: true,
          defaultValue: "",
          readOnly: false
        }
      ],
      history: {
        "1": {
          version: "1",
          created_at: 1610631878950,
          user_id: "21"
        },
        "2": {
          version: "2",
          created_at: 1612423020052,
          user_id: "39"
        },
        "3": {
          version: "3",
          created_at: 1612423342204,
          user_id: "39"
        },
        "4": {
          version: "4",
          created_at: 1612424320325,
          user_id: "39"
        },
        "5": {
          version: "5",
          created_at: 1612425095545,
          user_id: "39"
        }
      }
    };
    expect(helperFunctions.deleteEmptyRows(mockData)).toStrictEqual({
      columns: [
        {
          dataIndex: "column_1",
          defaultValue: "test",
          editable: true,
          id: "0",
          index: 1,
          inputType: "string",
          options: [],
          readOnly: false,
          required: true,
          title: "Column for distinct test"
        },
        {
          dataIndex: "column_2",
          defaultValue: "",
          editable: true,
          id: "1",
          index: 2,
          inputType: "string",
          options: [],
          readOnly: false,
          required: false,
          title: "Column for update test"
        },
        {
          dataIndex: "column_3",
          defaultValue: "test",
          editable: true,
          id: "2",
          index: 3,
          inputType: "string",
          options: [],
          readOnly: true,
          required: false,
          title: "A"
        },
        {
          dataIndex: "column_4",
          defaultValue: "",
          editable: true,
          id: "3",
          index: 4,
          inputType: "string",
          options: [],
          readOnly: false,
          required: false,
          title: "Column 4"
        },
        {
          dataIndex: "column_5",
          defaultValue: "",
          editable: true,
          id: "4",
          index: 5,
          inputType: "string",
          options: [],
          readOnly: false,
          required: false,
          title: "B"
        }
      ],
      created_at: 1610631878950,
      created_by: "21",
      history: {
        "1": {
          created_at: 1610631878950,
          user_id: "21",
          version: "1"
        },
        "2": {
          created_at: 1612423020052,
          user_id: "39",
          version: "2"
        },
        "3": {
          created_at: 1612423342204,
          user_id: "39",
          version: "3"
        },
        "4": {
          created_at: 1612424320325,
          user_id: "39",
          version: "4"
        },
        "5": {
          created_at: 1612425095545,
          user_id: "39",
          version: "5"
        }
      },
      id: "46eeb803-e450-4450-a2c0-05d8e754f475",
      name: "test for import table",
      rows: [
        {
          column_1: "a",
          column_2: "2020-09-30T23:45:31.943439Z",
          column_3: "1",
          column_4: "",
          column_5: "green",
          id: "0",
          index: 0
        },
        {
          column_1: "b",
          column_2: "09/06/2020",
          column_3: "2",
          column_4: "",
          column_5: "blue",
          id: "1",
          index: 1
        },
        {
          column_1: "a",
          column_2: "",
          column_3: "3",
          column_4: "",
          column_5: "green",
          id: "2",
          index: 2
        },
        {
          column_1: "c, d",
          column_2: "",
          column_3: "4",
          column_4: "",
          column_5: "blue",
          id: "3",
          index: 3
        },
        {
          column_1: "b",
          column_2: "Wed Dec 23 2020 08:50:22 GMT+0000 (GMT)",
          column_3: "5",
          column_4: "",
          column_5: "green",
          id: "4",
          index: 4
        },
        {
          column_1: "test",
          column_2: "",
          column_3: "test",
          column_4: "",
          column_5: "",
          id: "a24cde30-566e-11eb-8d7a-69434d69c3a2",
          index: 5
        }
      ],
      total_rows: 6,
      updated_at: 1612425095545,
      updated_by: "39",
      version: "5"
    });
  });

  test("mapColumnsWithRowsById should return correct data", () => {
    expect(
      helperFunctions.mapColumnsWithRowsById(
        [
          {
            column_1: "a",
            column_2: "2020-09-30T23:45:31.943439Z",
            column_3: "1",
            column_4: "",
            column_5: "green",
            id: "0",
            index: 0
          },
          {
            column_1: "b",
            column_2: "09/06/2020",
            column_3: "2",
            column_4: "",
            column_5: "blue",
            id: "1",
            index: 1
          },
          {
            column_1: "a",
            column_2: "",
            column_3: "3",
            column_4: "",
            column_5: "green",
            id: "2",
            index: 2
          },
          {
            column_1: "c, d",
            column_2: "",
            column_3: "4",
            column_4: "",
            column_5: "blue",
            id: "3",
            index: 3
          },
          {
            column_1: "b",
            column_2: "Wed Dec 23 2020 08:50:22 GMT+0000 (GMT)",
            column_3: "5",
            column_4: "",
            column_5: "green",
            id: "4",
            index: 4
          },
          {
            column_1: "test",
            column_2: "",
            column_3: "test",
            column_4: "",
            column_5: "",
            id: "a24cde30-566e-11eb-8d7a-69434d69c3a2",
            index: 5
          }
        ],
        [
          {
            dataIndex: "column_1",
            defaultValue: "test",
            editable: true,
            id: "0",
            index: 1,
            inputType: "string",
            options: [],
            readOnly: false,
            required: true,
            title: "Column for distinct test"
          },
          {
            dataIndex: "column_2",
            defaultValue: "",
            editable: true,
            id: "1",
            index: 2,
            inputType: "string",
            options: [],
            readOnly: false,
            required: false,
            title: "Column for update test"
          },
          {
            dataIndex: "column_3",
            defaultValue: "test",
            editable: true,
            id: "2",
            index: 3,
            inputType: "string",
            options: [],
            readOnly: true,
            required: false,
            title: "A"
          },
          {
            dataIndex: "column_4",
            defaultValue: "",
            editable: true,
            id: "3",
            index: 4,
            inputType: "string",
            options: [],
            readOnly: false,
            required: false,
            title: "Column 4"
          },
          {
            dataIndex: "column_5",
            defaultValue: "",
            editable: true,
            id: "4",
            index: 5,
            inputType: "string",
            options: [],
            readOnly: false,
            required: false,
            title: "B"
          }
        ]
      )
    ).toStrictEqual([
      {
        id: "0",
        title: "Column for distinct test",
        values: {
          row_0_0: "a",
          row_1_0: "b",
          row_2_0: "a",
          row_3_0: "c, d",
          row_4_0: "b",
          row_5_0: "test"
        }
      },
      {
        id: "1",
        title: "Column for update test",
        values: {
          row_0_1: "2020-09-30T23:45:31.943439Z",
          row_1_1: "09/06/2020",
          row_4_1: "Wed Dec 23 2020 08:50:22 GMT+0000 (GMT)"
        }
      },
      {
        id: "2",
        title: "A",
        values: {
          row_0_2: "1",
          row_1_2: "2",
          row_2_2: "3",
          row_3_2: "4",
          row_4_2: "5",
          row_5_2: "test"
        }
      },
      {
        id: "3",
        title: "Column 4",
        values: {}
      },
      {
        id: "4",
        title: "B",
        values: {
          row_0_4: "green",
          row_1_4: "blue",
          row_2_4: "green",
          row_3_4: "blue",
          row_4_4: "green"
        }
      }
    ]);
  });

  test("getFilteredRow should return correct data", () => {
    expect(
      helperFunctions.getFilteredRow(
        [],
        [
          {
            id: "0",
            index: 0,
            column_1: "a",
            column_2: "2020-09-30T23:45:31.943439Z",
            column_3: "1",
            column_4: "",
            column_5: "green"
          },
          {
            id: "1",
            index: 1,
            column_1: "b",
            column_2: "09/06/2020",
            column_3: "2",
            column_4: "",
            column_5: "blue"
          },
          {
            id: "2",
            index: 2,
            column_1: "a",
            column_2: "",
            column_3: "3",
            column_4: "",
            column_5: "green"
          },
          {
            id: "3",
            index: 3,
            column_1: "c, d",
            column_2: "",
            column_3: "4",
            column_4: "",
            column_5: "blue"
          },
          {
            id: "4",
            index: 4,
            column_1: "b",
            column_2: "Wed Dec 23 2020 08:50:22 GMT+0000 (GMT)",
            column_3: "5",
            column_4: "",
            column_5: "green"
          },
          {
            id: "5",
            index: 5,
            column_1: "test",
            column_2: "",
            column_3: "test",
            column_4: "",
            column_5: ""
          }
        ]
      )
    ).toStrictEqual({
      filterMap: { 0: 0, 1: 1, 2: 2, 3: 3, 4: 4, 5: 5 },
      rows: [
        {
          column_1: "a",
          column_2: "2020-09-30T23:45:31.943439Z",
          column_3: "1",
          column_4: "",
          column_5: "green",
          id: "0",
          index: 0
        },
        {
          column_1: "b",
          column_2: "09/06/2020",
          column_3: "2",
          column_4: "",
          column_5: "blue",
          id: "1",
          index: 1
        },
        {
          column_1: "a",
          column_2: "",
          column_3: "3",
          column_4: "",
          column_5: "green",
          id: "2",
          index: 2
        },
        {
          column_1: "c, d",
          column_2: "",
          column_3: "4",
          column_4: "",
          column_5: "blue",
          id: "3",
          index: 3
        },
        {
          column_1: "b",
          column_2: "Wed Dec 23 2020 08:50:22 GMT+0000 (GMT)",
          column_3: "5",
          column_4: "",
          column_5: "green",
          id: "4",
          index: 4
        },
        {
          column_1: "test",
          column_2: "",
          column_3: "test",
          column_4: "",
          column_5: "",
          id: "5",
          index: 5
        }
      ]
    });
  });

  test("validateHeaderNames should validated header names", () => {
    expect(
      helperFunctions.validateHeaderNames([
        {
          title: "aws"
        },
        {
          title: "abc"
        },
        {
          title: "xyz"
        }
      ])
    ).toStrictEqual(true);

    expect(
      helperFunctions.validateHeaderNames([
        {
          title: "aws"
        },
        {
          title: "aws"
        },
        {
          title: "xyz"
        }
      ])
    ).toStrictEqual(false);
  });

  test("validateHeaderCell should validate header cell", () => {
    expect(
      helperFunctions.validateHeaderCell(
        [
          {
            title: "aws",
            dataIndex: "qwer"
          },
          {
            title: "abc",
            dataIndex: "qwert"
          },
          {
            title: "xyz",
            dataIndex: "qwerty"
          }
        ],
        "qwerty",
        "aws"
      )
    ).toStrictEqual(false);

    expect(
      helperFunctions.validateHeaderCell(
        [
          {
            title: "aws",
            dataIndex: "qwer"
          },
          {
            title: "abc",
            dataIndex: "qwert"
          },
          {
            title: "xyz",
            dataIndex: "qwerty"
          }
        ],
        "qwerty",
        "xyz"
      )
    ).toStrictEqual(true);
  });
});
