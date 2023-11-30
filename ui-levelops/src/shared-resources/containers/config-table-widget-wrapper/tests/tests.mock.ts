export const configWidgetDataTransformMock = {
  parameters: {
    columns: [
      { id: "0", dataIndex: "column_1", index: 1, title: "Type", inputType: "string", options: [], editable: true },
      {
        id: "1",
        dataIndex: "column_2",
        index: 2,
        title: "Date (01/10/2020)",
        inputType: "string",
        options: [],
        editable: true
      },
      {
        id: "5",
        dataIndex: "column_3",
        index: 3,
        title: "Date (05/10/2020)",
        inputType: "string",
        options: [],
        editable: true,
        readOnly: false
      },
      {
        id: "6",
        dataIndex: "column_5",
        index: 5,
        title: "Date (07/10/2020)",
        inputType: "string",
        options: [],
        editable: true,
        readOnly: false
      },
      {
        id: "4",
        dataIndex: "column_6",
        index: 6,
        title: "Date (06/10/2020)",
        inputType: "string",
        options: [],
        editable: true,
        readOnly: false
      },
      {
        id: "2",
        dataIndex: "column_7",
        index: 7,
        title: "Date (02/10/2020)",
        inputType: "string",
        options: [],
        editable: true
      },
      {
        id: "3",
        dataIndex: "column_8",
        index: 8,
        title: "Date (03/10/2020)",
        inputType: "string",
        options: ["preset"],
        editable: true
      }
    ],
    rows: [
      {
        id: "db9680f0-07b9-11eb-a5a5-b5bc1d507d05",
        index: 0,
        column_1: "Developer (Frontend)",
        column_2: "1",
        column_7: "abc",
        column_8: "10",
        column_6: "85",
        column_3: "56",
        column_5: "123"
      },
      {
        id: "0aaa8170-07ba-11eb-a5a5-b5bc1d507d05",
        index: 1,
        column_1: "Developer (Backend)",
        column_2: "2",
        column_7: "3",
        column_8: "11",
        column_6: "aasd",
        column_3: "567",
        column_5: "36"
      },
      {
        id: "2457afd0-07ba-11eb-a5a5-b5bc1d507d05",
        index: 2,
        column_1: "Tester",
        column_2: "3",
        column_7: "4",
        column_8: "12",
        column_6: "asda",
        column_3: "897",
        column_5: "58"
      }
    ],
    xAxis: "0",
    yAxis: [{ key: "1", value: "bar", label: "" }],
    yUnit: "",
    filters: {}
  },
  response: {
    data: [
      { name: "Developer (Frontend)", "1_bar": 1 },
      { name: "Developer (Backend)", "1_bar": 2 },
      { name: "Tester", "1_bar": 3 }
    ],
    chart_props: { unit: "", chartProps: { barGap: 0, margin: { top: 20, right: 5, left: 5, bottom: 50 } } }
  }
};

export const configureWidgetTransformMock2 = {
  parameters: {
    columns: [
      { id: "0", dataIndex: "column_1", index: 1, title: "Type", inputType: "string", options: [], editable: true },
      {
        id: "1",
        dataIndex: "column_2",
        index: 2,
        title: "Date (01/10/2020)",
        inputType: "string",
        options: [],
        editable: true
      },
      {
        id: "5",
        dataIndex: "column_3",
        index: 3,
        title: "Date (05/10/2020)",
        inputType: "string",
        options: [],
        editable: true,
        readOnly: false
      },
      {
        id: "6",
        dataIndex: "column_5",
        index: 5,
        title: "Date (07/10/2020)",
        inputType: "string",
        options: [],
        editable: true,
        readOnly: false
      },
      {
        id: "4",
        dataIndex: "column_6",
        index: 6,
        title: "Date (06/10/2020)",
        inputType: "string",
        options: [],
        editable: true,
        readOnly: false
      },
      {
        id: "2",
        dataIndex: "column_7",
        index: 7,
        title: "Date (02/10/2020)",
        inputType: "string",
        options: [],
        editable: true
      },
      {
        id: "3",
        dataIndex: "column_8",
        index: 8,
        title: "Date (03/10/2020)",
        inputType: "string",
        options: ["preset"],
        editable: true
      }
    ],
    rows: [
      {
        id: "db9680f0-07b9-11eb-a5a5-b5bc1d507d05",
        index: 0,
        column_1: "Developer (Frontend)",
        column_2: "1",
        column_7: "abc",
        column_8: "10",
        column_6: "85",
        column_3: "56",
        column_5: "123"
      },
      {
        id: "0aaa8170-07ba-11eb-a5a5-b5bc1d507d05",
        index: 1,
        column_1: "Developer (Backend)",
        column_2: "2",
        column_7: "3",
        column_8: "11",
        column_6: "aasd",
        column_3: "567",
        column_5: "36"
      },
      {
        id: "2457afd0-07ba-11eb-a5a5-b5bc1d507d05",
        index: 2,
        column_1: "Tester",
        column_2: "3",
        column_7: "4",
        column_8: "12",
        column_6: "asda",
        column_3: "897",
        column_5: "58"
      }
    ],
    xAxis: "0",
    yAxis: [
      { key: "1", value: "bar", label: "" },
      { key: "5", value: "line", label: "" }
    ],
    yUnit: "",
    filters: {}
  },
  response: {
    data: [
      { name: "Developer (Frontend)", "1_bar": 1, "5_line": 56 },
      { name: "Developer (Backend)", "1_bar": 2, "5_line": 567 },
      { name: "Tester", "1_bar": 3, "5_line": 897 }
    ],
    chart_props: { unit: "", chartProps: { barGap: 0, margin: { top: 20, right: 5, left: 5, bottom: 50 } } }
  }
};

export const configWidgetTransformMock3 = {
  parameters: {
    columns: [
      { id: "0", dataIndex: "column_1", index: 1, title: "Type", inputType: "string", options: [], editable: true },
      {
        id: "1",
        dataIndex: "column_2",
        index: 2,
        title: "Date (01/10/2020)",
        inputType: "string",
        options: [],
        editable: true
      },
      {
        id: "5",
        dataIndex: "column_3",
        index: 3,
        title: "Date (05/10/2020)",
        inputType: "string",
        options: [],
        editable: true,
        readOnly: false
      },
      {
        id: "6",
        dataIndex: "column_5",
        index: 5,
        title: "Date (07/10/2020)",
        inputType: "string",
        options: [],
        editable: true,
        readOnly: false
      },
      {
        id: "4",
        dataIndex: "column_6",
        index: 6,
        title: "Date (06/10/2020)",
        inputType: "string",
        options: [],
        editable: true,
        readOnly: false
      },
      {
        id: "2",
        dataIndex: "column_7",
        index: 7,
        title: "Date (02/10/2020)",
        inputType: "string",
        options: [],
        editable: true
      },
      {
        id: "3",
        dataIndex: "column_8",
        index: 8,
        title: "Date (03/10/2020)",
        inputType: "string",
        options: ["preset"],
        editable: true
      }
    ],
    rows: [
      {
        id: "db9680f0-07b9-11eb-a5a5-b5bc1d507d05",
        index: 0,
        column_1: "Developer (Frontend)",
        column_2: "1",
        column_7: "abc",
        column_8: "10",
        column_6: "85",
        column_3: "56",
        column_5: "123"
      },
      {
        id: "0aaa8170-07ba-11eb-a5a5-b5bc1d507d05",
        index: 1,
        column_1: "Developer (Backend)",
        column_2: "2",
        column_7: "3",
        column_8: "11",
        column_6: "aasd",
        column_3: "567",
        column_5: "36"
      },
      {
        id: "2457afd0-07ba-11eb-a5a5-b5bc1d507d05",
        index: 2,
        column_1: "Tester",
        column_2: "3",
        column_7: "4",
        column_8: "12",
        column_6: "asda",
        column_3: "897",
        column_5: "58"
      }
    ],
    xAxis: "0",
    yAxis: [],
    yUnit: "",
    filters: { "1": "1" }
  },
  response: {
    data: [{ name: "Developer (Frontend)", count: 1 }],
    chart_props: {
      barProps: [{ name: "count", dataKey: "count", unit: "count" }],
      unit: "Count",
      chartProps: { barGap: 0, margin: { top: 20, right: 5, left: 5, bottom: 50 } }
    }
  }
};

export const configTableStatTransform = {
  parameters: {
    columns: [
      { id: "0", dataIndex: "column_1", index: 1, title: "Type", inputType: "string", options: [], editable: true },
      {
        id: "1",
        dataIndex: "column_2",
        index: 2,
        title: "Date (01/10/2020)",
        inputType: "string",
        options: [],
        editable: true
      },
      {
        id: "5",
        dataIndex: "column_3",
        index: 3,
        title: "Date (05/10/2020)",
        inputType: "string",
        options: [],
        editable: true,
        readOnly: false
      },
      {
        id: "6",
        dataIndex: "column_5",
        index: 5,
        title: "Date (07/10/2020)",
        inputType: "string",
        options: [],
        editable: true,
        readOnly: false
      },
      {
        id: "4",
        dataIndex: "column_6",
        index: 6,
        title: "Date (06/10/2020)",
        inputType: "string",
        options: [],
        editable: true,
        readOnly: false
      },
      {
        id: "2",
        dataIndex: "column_7",
        index: 7,
        title: "Date (02/10/2020)",
        inputType: "string",
        options: [],
        editable: true
      },
      {
        id: "3",
        dataIndex: "column_8",
        index: 8,
        title: "Date (03/10/2020)",
        inputType: "string",
        options: ["preset"],
        editable: true
      }
    ],
    rows: [
      {
        id: "db9680f0-07b9-11eb-a5a5-b5bc1d507d05",
        index: 0,
        column_1: "Developer (Frontend)",
        column_2: "1",
        column_7: "abc",
        column_8: "10",
        column_6: "85",
        column_3: "56",
        column_5: "123"
      },
      {
        id: "0aaa8170-07ba-11eb-a5a5-b5bc1d507d05",
        index: 1,
        column_1: "Developer (Backend)",
        column_2: "2",
        column_7: "3",
        column_8: "11",
        column_6: "aasd",
        column_3: "567",
        column_5: "36"
      },
      {
        id: "2457afd0-07ba-11eb-a5a5-b5bc1d507d05",
        index: 2,
        column_1: "Tester",
        column_2: "3",
        column_7: "4",
        column_8: "12",
        column_6: "asda",
        column_3: "897",
        column_5: "58"
      }
    ],
    xAxis: "",
    yAxis: [{ key: "1" }],
    yUnit: "test",
    filters: {}
  },
  response: { stat: 6, unit: "test" }
};
