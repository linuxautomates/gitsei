import {toBarGraph, toGraph} from "../graphUtils"

describe("graphUtils",()=>{

describe('toGraph function', () => {
  it('should convert series to graph format with specified axes', () => {
    const series = [10, 20, 30, 40];
    const xaxis = 'time';
    const yaxis = 'value';

    const expectedGraph = [
      { time: 0, value: 10 },
      { time: 1, value: 20 },
      { time: 2, value: 30 },
      { time: 3, value: 40 },
    ];

    const result = toGraph(series, xaxis, yaxis);
    expect(result).toEqual(expectedGraph);
  });

  it('should handle empty series and return an empty array', () => {
    const series = [];
    const xaxis = 'time';
    const yaxis = 'value';

    const result = toGraph(series, xaxis, yaxis);
    expect(result).toEqual([]);
  });

  it('should handle series with negative numbers', () => {
    const series = [-10, -20, -30];
    const xaxis = 'index';
    const yaxis = 'magnitude';

    const expectedGraph = [
      { index: 0, magnitude: -10 },
      { index: 1, magnitude: -20 },
      { index: 2, magnitude: -30 },
    ];

    const result = toGraph(series, xaxis, yaxis);
    expect(result).toEqual(expectedGraph);
  });

});
     describe("toBarGraph function",()=>{
        it("should convert graph to bar graph",()=>{
            const data={
                'A': 12,
                'B': 16,
                'C': 20
            }
            const result=toBarGraph(data);
            expect(result).toEqual([
                {
                    'name':'A',
                    'value': 12
                },
                {
                    'name':'B',
                    'value': 16
                },
                {
                    'name':'C',
                    'value': 20
                },
            ]);
        })
        it("should handle empty data",()=>{
            const data={}
            const result=toBarGraph(data);
            expect(result).toEqual([]);
        })
     })
})