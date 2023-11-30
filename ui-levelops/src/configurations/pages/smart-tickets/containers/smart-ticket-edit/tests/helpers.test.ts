import { updatedWorkItem } from "../helper";
import { RestWorkItem } from "classes/RestWorkItem";

describe("SmartTickerEditHelpers", () => {
  test("updatedWorkItem should return correct values", () => {
    const workItem = new RestWorkItem();
    workItem.title = "test";

    const data = {
      attachments: [
        {
          response: {
            id: "test_response_id",
            file_name: "test attachment"
          }
        },
        {
          file_name: "test attachment 2",
          upload_id: "test_id"
        }
      ],
      assignees: [
        { key: "key1", label: "assignee1@test.com" },
        { key: "key2", label: "assignee2@test.com" }
      ],
      templateFields: [],
      values: {},
      tags: [{ key: "tag1", value: "tag1" }],
      ticketType: "AUTOMATED",
      workItem
    };

    const expectedValue = new RestWorkItem({ ...workItem.json() });
    expectedValue.assignees = [
      { user_email: "assignee1@test.com", user_id: "key1" },
      { user_email: "assignee2@test.com", user_id: "key2" }
    ];
    expectedValue.attachments = [
      {
        file_name: "test attachment",
        upload_id: "test_response_id"
      },
      {
        file_name: "test attachment 2",
        upload_id: "test_id"
      }
    ];
    expectedValue.ticket_data_values = [];
    expectedValue.tag_ids = ["tag1"];
    expectedValue.ticket_type = "AUTOMATED";

    const returnValue = updatedWorkItem(data);
    expect(returnValue.json()).toEqual(expectedValue.json());
  });

  test("updatedWorkItem should not break if empty values are provided", () => {
    const workItem = new RestWorkItem();

    const data = {
      attachments: [],
      assignees: [],
      templateFields: [],
      values: {},
      tags: [],
      ticketType: "AUTOMATED",
      workItem
    };

    const expectedValue = new RestWorkItem({ ...workItem.json() });
    expectedValue.assignees = [];
    expectedValue.attachments = [];
    expectedValue.ticket_data_values = [];
    expectedValue.tag_ids = [];
    expectedValue.ticket_type = "AUTOMATED";

    const returnValue = updatedWorkItem(data);
    expect(returnValue.json()).toEqual(expectedValue.json());
  });

  test("updatedWorkItem should throw error if empty object or other data types are passed", () => {
    expect(() => updatedWorkItem({})).toThrow("Cannot read property 'json' of undefined");
    expect(() => updatedWorkItem("test")).toThrow("Cannot read property 'json' of undefined");
    expect(() => updatedWorkItem(123)).toThrow("Cannot read property 'json' of undefined");
    expect(() => updatedWorkItem(true)).toThrow("Cannot read property 'json' of undefined");
  });
});
