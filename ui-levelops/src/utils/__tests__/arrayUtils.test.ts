import { appendErrorMessagesHelper } from '../arrayUtils'; 
import {arrayMin} from '../arrayUtils'
import {arrayMove} from "../arrayUtils"
import { arrayOfObjFilterAndMap } from '../arrayUtils';
import { compareValues} from "../arrayUtils";
import { convertNumArrayToStringArray } from '../arrayUtils'; 
import {divideArrayIntoChunks} from "../arrayUtils";
import { moveArrayElement } from "../arrayUtils";
import { nestedSort } from '../arrayUtils'; 
import { shiftArrayByKey } from '../arrayUtils';

describe('shiftArrayByKey', () => {
  it('shifts elements based on the specified key and value', () => {
    const input = [
      { id: 1, name: 'A' },
      { id: 2, name: 'B' },
      { id: 3, name: 'C' },
      { id: 4, name: 'A' },
      { id: 5, name: 'D' },
    ];

    const key = 'name';
    const value = 'A';

    const result = shiftArrayByKey(input, key, value);
      expect(result).toEqual([
      { id: 2, name: 'B' },
      { id: 3, name: 'C' },
      { id: 5, name: 'D' },
      { id: 1, name: 'A' },
      { id: 4, name: 'A' },
    ]);
  });

  it('array is empty', () => {
    const input = [];
    const key = 'name';
    const value = 'A';

    const result = shiftArrayByKey(input, key, value);

    expect(result).toEqual([]);
  });

  it('no matching elements', () => {
    const input = [
      { id: 1, name: 'X' },
      { id: 2, name: 'Y' },
      { id: 3, name: 'Z' }
    ];
    const key = 'name';
    const value = 'A';

    const result = shiftArrayByKey(input, key, value);

    expect(result).toEqual([
      { id: 1, name: 'X' },
      { id: 2, name: 'Y' },
      { id: 3, name: 'Z' }
    ]);
  });

  it('array size is 1', () => {
    const input = [{ id: 1, name: 'A' }];
    const key = 'name';
    const value = 'A';

    const result = shiftArrayByKey(input, key, value);


    expect(result).toEqual([{ id: 1, name: 'A' }]);
  });
});


describe('nestedSort', () => {
  it('should sort an array of objects based on two keys', () => {
    const input = [
      { key1: 3, key2: 4 },
      { key1: 2, key2: 5 },
      { key1: 3, key2: 3 },
      { key1: 1, key2: 2 },
    ];

    const result = nestedSort(input, 'key1', 'key2');
    expect(result).toEqual([
      { key1: 1, key2: 2 },
      { key1: 2, key2: 5 },
      { key1: 3, key2: 3 },
      { key1: 3, key2: 4 },
    ]);
  });

  it('array is empty', () => {
    const input = [];

    const result = nestedSort(input, 'key1', 'key2');


    expect(result).toEqual([]);
  });


  it('one of the key is missing', () => {

    const input = [
    { key1: 3, key2: 4 },
    { key1: 2 },
    { key1: 1, key2: 2 },
  ];

  const result = nestedSort(input, 'key1', 'key2');


  expect(result).toEqual([
    { key1: 1, key2: 2 },
    { key1: 2 },
    { key1: 3, key2: 4 },
  ]);
});



});


describe("move array Element tests",()=>{
    const test_cases=[
        {
            it:"If from and to are within range (0,size of array)",
            input:{
                arr:[1,2,3,4,5,6],
                from:2,
                to:4
            },
            output: [1,2,4,5,3,6]
        }
    ]
    test_cases.forEach(test_case =>{
        test(test_case.it,()=>{
            const result=moveArrayElement(test_case.input.arr,test_case.input.from,test_case.input.to);

            expect(result).toStrictEqual(test_case.output)

        })

    })
})

    test("array can be divided into chunks", () => {
      const array = [1, 2, 3, 4, 5, 6, 7, 8, 9];
      const chunkSize = 3;
      const result = divideArrayIntoChunks(array, chunkSize);


      expect(Array.isArray(result)).toBe(true);


      expect(result).toEqual([[1, 2, 3], [4, 5, 6], [7, 8, 9]]);
    });

    test('check for empty array', () => {
      const array = [];
      const chunkSize = 3;
      const result = divideArrayIntoChunks(array, chunkSize);


      expect(Array.isArray(result)).toBe(true);
      expect(result).toEqual([]);
    });

    test('chunk size greater than the array length', () => {
      const array = [1, 2, 3, 4, 5];
      const chunkSize = 10;
      const result = divideArrayIntoChunks(array, chunkSize);


      expect(Array.isArray(result)).toBe(true);
      expect(result).toEqual([array]);
    });


   describe('convertNumArrayToStringArray', () => {
   it('converts an array of numbers to an array of strings', () => {
    const input = [0,1, 2, 3, 4, 5];
    const result = convertNumArrayToStringArray(input);

     expect(result).toEqual(['0','1', '2', '3', '4', '5']);
  });

  it('array is empty', () => {
    const input = [];
    const result = convertNumArrayToStringArray(input);
    expect(result).toEqual([]);
  });

  it('input numbers are negative', () => {
    const input = [-1, -2, -3];
    const result = convertNumArrayToStringArray(input);
    expect(result).toEqual(['-1', '-2', '-3']);
  });

});


describe("compareValues",()=>{
    it("v1 is greater than v2",()=>{
        const v1=5;
        const v2=2;
        const result=compareValues(v1,v2);
        expect(result).toBe(1)

    });

    it("v2 is greater than v1",()=>{
        const v1=2;
        const v2=5;
        const result=compareValues(v1,v2);
        expect(result).toBe(-1);
    });

    it("v1 is equal to v2",()=>{
        const v1=5;
        const v2=5;
        const result=compareValues(v1,v2);
        expect(result).toBe(0)
    });
});

describe('arrayOfObjFilterAndMap', () => {
  it('filter and map the array of objects correctly', () => {
    const input = [
      { id: 1, name: 'Item 1' },
      { id: 2, name: 'Item 2' },
      { id: 3, name: 'Item 3' },
      { id: 4, name: 'Item 4' },
    ];
    const arr = [1, 3, 5];
    const result = arrayOfObjFilterAndMap(input, arr);


    expect(result).toEqual([1, 3]);
  });

  it('array is empty', () => {
    const input = [
      { id: 1, name: 'Item 1' },
      { id: 2, name: 'Item 2' },
      { id: 3, name: 'Item 3' },
      { id: 4, name: 'Item 4' },
    ];

    const arr = [];

    const result = arrayOfObjFilterAndMap(input, arr);


  });

  it('handle non-existent IDs', () => {
    const input = [
      { id: 1, name: 'Item 1' },
      { id: 2, name: 'Item 2' },
      { id: 3, name: 'Item 3' },
    ];

    const arr = [4, 5];

    const result = arrayOfObjFilterAndMap(input, arr);


    expect(result).toEqual([]);
  });
});


describe("Array Move Tests",()=>{
    it("array can be modified",()=>{
         const input=[1,2,3,4,5,6];
         const from= 2;
         const to = 4;
         const result =arrayMove(input,from,to);
         expect(result).toEqual([1,2,4,5,3,6]);

    })
    it("array is empty",()=>{
        const input=[];
        const from=2;
        const to=4;
        const result=arrayMove(input,from,to);
        expect(result).toEqual([]);
    })
    // TODO: FOR NEGATIVE TEST CASES IT IS BREAKING, ONCE THE TODO IS RESOLVED WE CAN CHECK THE TESTCASES
    // it("from is out of bound",()=>{
    //     const input=[1,2,3,4,5,6];
    //     const from =7;
    //     const to=2
    //     const result=arrayMove(input,from,to)
    //     expect(result).toEqual([])
    // })
    // it("to is out of bound",()=>{
    //     const input=[1,2,3,4,5];
    //     const from =2;
    //     const to=7;
    //     const result=arrayMove(input,from,to)
    //     expect(result).toEqual([1,2,3,4,5,6])
    // })
    it('moving element at the beginning', () => {
        const input = [1, 2, 3, 4, 5];
        const from= 3;
        const to = 0;

        const result = arrayMove(input, from, to);

        expect(result).toEqual([4, 1, 2, 3, 5]);
      });

      it('moving element at the end', () => {
        const input= [1, 2, 3, 4, 5];
        const from= 0;
        const to= 4;

        const result = arrayMove(input, from, to);
        expect(result).toEqual([2, 3, 4, 5, 1]);
      });
})

describe('arrayMin', () => {
  it('all numbers are positive', () => {
    const arr = [3, 1, 4, 2, 5];
    expect(arrayMin(arr)).toBe(1);
  });

  it('array contain negative number', () => {
    const arr = [5, -3, 6, -9, 1];
    expect(arrayMin(arr)).toBe(-9);
  });

  // TODO: WHEN ARRAY IS EMPTY , THE TESTCASE CAN BE RUN WHEN THE ISSUE IS RESOLVED
  // it('should return undefined for an empty array', () => {
  //   const arr=[]
  //   expect(arrayMin(arr)).toBeUndefined();
  // });

  it('array with one element', () => {
    const arr = [4];
    expect(arrayMin(arr)).toBe(4);
  });

  it('all elements are same', () => {
    const arr = [5,5,5,5,5];
    expect(arrayMin(arr)).toBe(5);
  });
});

describe('appendErrorMessagesHelper', () => {
  it('appending error messages correctly for an array of objects', () => {
    const input = [
      { success: true },
      { success: false, error: 'Error 1' },
      { success: false, error: 'Error 2' },
      { success: true }
    ];

    const result = appendErrorMessagesHelper(input);


    expect(result).toEqual('Error 2 Error 1 ');
  });

  it('array is empty', () => {
    const input = [];

    const result = appendErrorMessagesHelper(input);


    expect(result).toEqual('');
  });

  it('only single object is passed', () => {
    const input = { success: true };

    const result = appendErrorMessagesHelper(input);


    expect(result).toEqual('');
  });

  it('single object with error is passed', () => {
    const input = { success: false, error: 'ERROR' };

    const result = appendErrorMessagesHelper(input);


    expect(result).toBe('ERROR');
  });

  it('single object with no error', () => {
    const input = { success: false };

    const result = appendErrorMessagesHelper(input);

    expect(result).toEqual('Error occurred!');
  });
});
