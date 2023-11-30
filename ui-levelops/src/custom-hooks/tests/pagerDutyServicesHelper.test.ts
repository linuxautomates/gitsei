import { pagerdutyServicesTransformer } from "custom-hooks/helpers/pagerDutyServices.helper";

describe("pagerdutyServicesHelpers", () => {
  test("pagerdutyServicesTransformer should return correct value for pagerduty hotspot report", () => {
    const data = {
      reportType: "pagerduty_hotspot_report",
      apiData: [
        {
          id: "test1",
          name: "test 1",
          type: "pagerduty",
          aggregations: [
            { key: "P1", count: 1 },
            { key: "P3", count: 1 },
            { key: "P0", count: 2 }
          ]
        },
        {
          id: "test2",
          name: "test 2",
          type: "pagerduty",
          aggregations: [
            { key: "P4", count: 1 },
            { key: "P2", count: 1 },
            { key: "P0", count: 2 }
          ]
        }
      ]
    };

    const expectedValue = {
      data: [
        {
          P0: 2,
          P1: 1,
          P3: 1,
          name: "test 1"
        },
        {
          P0: 2,
          P2: 1,
          P4: 1,
          name: "test 2"
        }
      ]
    };

    const returnValue = pagerdutyServicesTransformer(data);
    expect(returnValue).toEqual(expectedValue);
  });

  test("pagerdutyServicesTransformer should return correct value for pagerduty release incidents report", () => {
    const data = {
      reportType: "pagerduty_release_incidents",
      apiData: [
        {
          from: 1511681465,
          to: 1611661465,
          incidents_count: 10,
          alerts_count: 5
        },
        {
          from: 1511681465,
          to: 1611681465,
          incidents_count: 100,
          alerts_count: 56
        }
      ]
    };

    const expectedValue = {
      data: [
        {
          alerts_count: 5,
          incidents_count: 10,
          name: "2021-01-26 - 2017-11-26"
        },
        {
          alerts_count: 56,
          incidents_count: 100,
          name: "2021-01-26 - 2017-11-26"
        }
      ]
    };

    const returnValue = pagerdutyServicesTransformer(data);
    expect(returnValue).toEqual(expectedValue);
  });

  test("pagerdutyServicesTransformer should return correct value for pagerduty ack trend report", () => {
    const data = {
      reportType: "pagerduty_ack_trend",
      apiData: [
        {
          aggregations: [
            { value: 492, key: 1603238400 },
            { value: 568, key: 1603152000 },
            { value: 582, key: 1603065600 },
            { value: 498, key: 1602979200 }
          ],
          name: "Test"
        },
        {
          aggregations: [
            { value: 500, key: 1603238400 },
            { value: 568, key: 1603152000 }
          ],
          name: "Test 2"
        }
      ]
    };

    const expectedValue = {
      data: [
        {
          "2020-10-18": 498,
          "2020-10-19": 582,
          "2020-10-20": 568,
          "2020-10-21": 492,
          name: "Test"
        },
        {
          "2020-10-20": 568,
          "2020-10-21": 500,
          name: "Test 2"
        }
      ]
    };

    const returnValue = pagerdutyServicesTransformer(data);
    expect(returnValue).toEqual(expectedValue);
  });

  test("pagerdutyServicesTransformer should return correct value for pagerduty after hours report", () => {
    const data = {
      reportType: "pagerduty_after_hours",
      apiData: [
        {
          after_hours_minutes: 300,
          name: "Test User 1"
        },
        {
          after_hours_minutes: 150,
          name: "Test User 2"
        }
      ]
    };

    const expectedValue = {
      data: [
        {
          name: "Test User 1",
          value: 300
        },
        {
          name: "Test User 2",
          value: 150
        }
      ]
    };

    const returnValue = pagerdutyServicesTransformer(data);
    expect(returnValue).toEqual(expectedValue);
  });

  test("pagerdutyServicesTransformer should return undefined value if report type is not passed", () => {
    const data = {
      apiData: [
        {
          after_hours_minutes: 300,
          name: "Test User 1"
        }
      ]
    };

    const expectedValue = {
      data: undefined
    };

    const returnValue = pagerdutyServicesTransformer(data);
    expect(returnValue).toEqual(expectedValue);
  });
});
