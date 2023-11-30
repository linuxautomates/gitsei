import { sortingColumnForFixedLeft } from "../helper";
import { get, forEach } from "lodash";
import * as mockData from "../__mocks__/helpers.mocks.json";

describe("server paginated helper Tests", () => {
    test("sorting of column witch have fixed left align in column array responses", () => {
        const parsedMockData = JSON.parse(JSON.stringify(mockData));
        const functionTestCases = get(parsedMockData, ["sortingColumnForFixedLeft"], []);
        forEach(functionTestCases, testCase => {                        
            const currentOutput = sortingColumnForFixedLeft(testCase.input);            
            expect(currentOutput).toStrictEqual(testCase.output);
        });
    });
});


