import * as helpers from "custom-hooks/helpers";

describe("sankeyHelpers", () => {
  test("getProps should return correct values", () => {
    const value = 100;
    const maxValue = 400;
    const scale = 1.5;
    const index = 2;

    const returnValue = helpers.getProps(value, maxValue, scale, index);
    expect(returnValue).toEqual({
      pathProps: {
        fill: "none",
        stroke: "#F7E8D9",
        strokeWidth: Math.max((value / maxValue) * scale, 0.02 * scale)
      },
      nodeProps: {
        height: (value / maxValue) * scale,
        fill: "#dd8b39"
      },
      textProps: {
        textAnchor: "start",
        fontSize: "16px",
        fontWeight: 500,
        x: 5
      }
    });
  });
});
