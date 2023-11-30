import * as helperFunctions from "./helper";

describe("propelValidation test suite", () => {
  test("it should return correct error message if no nodes are present in propel object", () => {
    const mock_propel = {
      ui_data: {}
    };
    const mock_response = { error: "No nodes found!" };

    expect(helperFunctions.propelValidation(mock_propel)).toStrictEqual(mock_response);
  });

  test("it should return correct error message if no links are present in propel object", () => {
    const mock_propel = {
      ui_data: {
        nodes: {
          node1: {}
        }
      }
    };
    const mock_response = { error: "No links found!" };

    expect(helperFunctions.propelValidation(mock_propel)).toStrictEqual(mock_response);
  });

  test("it should return correct error message if less that 2 nodes are present in propel object", () => {
    const mock_propel = {
      ui_data: {
        nodes: {
          node1: {}
        },
        links: {}
      }
    };
    const mock_response = { error: "Propel must have minimum 2 nodes" };

    expect(helperFunctions.propelValidation(mock_propel)).toStrictEqual(mock_response);
  });

  test("it should return correct error message if less that 1 links are present in propel object", () => {
    const mock_propel = {
      ui_data: {
        nodes: {
          node1: {},
          node2: {}
        },
        links: {}
      }
    };
    const mock_response = { error: "Propel must have minimum 1 link" };

    expect(helperFunctions.propelValidation(mock_propel)).toStrictEqual(mock_response);
  });

  test("it should return correct error message if links are not connected to nodes", () => {
    const mock_propel = {
      ui_data: {
        nodes: {
          node1: { id: 1 },
          node2: { id: 2 }
        },
        links: {
          link1: {
            to: { nodeId: 3 },
            from: { nodeId: 4 }
          }
        }
      }
    };
    const mock_response = { error: "All links must be connected to node" };

    expect(helperFunctions.propelValidation(mock_propel)).toStrictEqual(mock_response);
  });

  test("it should return correct error message if all links are connected to nodes", () => {
    const mock_propel = {
      ui_data: {
        nodes: {
          node1: { id: 1 },
          node2: { id: 2 }
        },
        links: {
          link1: {
            to: { nodeId: 1 },
            from: { nodeId: 2 }
          }
        }
      }
    };
    const mock_response = { error: "" };

    expect(helperFunctions.propelValidation(mock_propel)).toStrictEqual(mock_response);
  });
});
