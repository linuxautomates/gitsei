import * as helpers from "custom-hooks/helpers";

describe("multiplexDataHelpers", () => {
  test("multiplexData should return correct values", () => {
    const dataHash = {
      test_id: {
        data: [
          {
            key: "test1",
            median: 10,
            min: 0,
            max: 517,
            name: "test 1"
          },
          {
            key: "test2",
            median: 33,
            min: 0,
            max: 300,
            name: "test 2"
          },
          {
            key: "test3",
            median: 20,
            min: 0,
            max: 400,
            name: "test 3"
          },
          {
            key: "test4",
            median: 60,
            min: 0,
            max: 600,
            name: "test 4"
          }
        ]
      }
    };

    const apiCallArray: any[] = [
      {
        apiData: undefined,
        apiMethod: "list",
        apiName: "test_api",
        apiStatUri: "test_uri",
        childName: "",
        composite: true,
        filters: {
          filter: { integration_ids: ["integration_id_1", "integration_id_2"], product_id: "product_id" },
          across: "test_across"
        },
        id: "test_id",
        localFilters: { across: "test_across" },
        maxRecords: 20,
        reportType: "test_report",
        sortBy: undefined
      }
    ];

    const returnValue = helpers.multiplexData(dataHash, apiCallArray);

    expect(returnValue).toEqual({
      data: [
        {
          key: "test1",
          "median-0": 10,
          name: "test 1"
        },
        {
          key: "test2",
          "median-0": 33,
          name: "test 2"
        },
        {
          key: "test3",
          "median-0": 20,
          name: "test 3"
        },
        {
          key: "test4",
          "median-0": 60,
          name: "test 4"
        }
      ]
    });
  });

  test("multiplexData should return undefined if dataHash is null or undefined", () => {
    expect(
      helpers.multiplexData(undefined, [
        {
          apiData: undefined,
          apiMethod: "list",
          apiName: "test_api",
          apiStatUri: "test_uri",
          childName: "",
          composite: true,
          filters: {
            filter: { integration_ids: ["integration_id_1", "integration_id_2"], product_id: "product_id" },
            across: "test_across"
          },
          id: "test_id",
          localFilters: { across: "test_across" },
          maxRecords: 20,
          reportType: "test_report",
          sortBy: undefined
        }
      ])
    ).toBe(undefined);
  });

  test("multiplexData should return key value if dataHash has one key and composite value is false or undefined", () => {
    expect(
      helpers.multiplexData(
        {
          key: "value"
        },
        [
          {
            composite: false,
            id: "test_id"
          }
        ]
      )
    ).toBe("value");
  });
});
