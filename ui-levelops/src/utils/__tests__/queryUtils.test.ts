import exp from 'constants'
import {buildQueryParamsFromObject,parseQueryParamsIntoKeys} from '../queryUtils'

describe("queryUtils",()=>{
    describe("buildQueryFromObject",()=>{
        it("value is a string",()=>{
            const obj={
                'key1':'val1',
                'key2': 'val2'
            }
            const result=buildQueryParamsFromObject(obj);
            expect(result).toEqual("key1=val1&key2=val2");
        })
        it("value is undefined",()=>{
            const obj={
                'key1':'11',
                'key2': undefined
            }
            const result=buildQueryParamsFromObject(obj);
            expect(result).toEqual('key1=11');
        })
        it("value is an array of number",()=>{
            const obj={
                'key1': [11,122,31,34],
                'key2' :'ABC'
            }
            const result=buildQueryParamsFromObject(obj);
            expect(result).toEqual("key1=11%2C122%2C31%2C34&key2=ABC");
        })
        it('value is empty',()=>{
            const obj={
                'key1':''
            }
            const result=buildQueryParamsFromObject(obj);
            expect(result).toBe('')
        })
    })
    describe("parseQueryParamsIntoKeys",()=>{

       
          it('returns an empty object when keys are not provided', () => {
            expect(parseQueryParamsIntoKeys('?param1=value1', undefined)).toEqual({});
          });
        
          it('returns an empty object when search is empty', () => {
            expect(parseQueryParamsIntoKeys('', ['param1'])).toEqual({});
          });
        
          it('correctly parses simple query strings', () => {
            const result = parseQueryParamsIntoKeys('?param1=value1&param2=value2', ['param1', 'param2']);
            expect(result).toEqual({ param1: ['value1'], param2: ['value2'] });
          });
        
          it('handles "filters" key correctly', () => {
            const result = parseQueryParamsIntoKeys('?param1=value1&filters={"param2":"value2"}', ['param1', 'param2']);
            expect(result).toEqual({ param1: ['value1'],'param2':['value2'] });
          });
        
          it('handles different types of values in query string', () => {
            const result = parseQueryParamsIntoKeys('?param1=1&param2={"key":"value"}&param3=1,2,3', ['param1', 'param2', 'param3']);
            expect(result).toEqual({ param1: ['1'],"param2":[
                "{\"key\":\"value\"}"], param3: ['1', '2', '3'] });
          });
        
          it('handles empty values for keys in the query string', () => {
            const result = parseQueryParamsIntoKeys('?param1=&param2=empty&param3=', ['param1', 'param2', 'param3']);
            expect(result).toEqual({ param2: ['empty']});
          });
        
          it('handles undefined values for keys in the query string', () => {
            const result = parseQueryParamsIntoKeys('?param1&param2=undefined&param3', ['param1', 'param2', 'param3']);
            expect(result).toEqual({ param1: undefined, param2: ['undefined'], param3: undefined });
          });
        
          it('handles unexpected or malformed query string', () => {
            const result = parseQueryParamsIntoKeys('?param1&param2===value&&&&param3=1,2,3&invalidJson={invalid}', ['param1', 'param2', 'param3', 'invalidJson']);
            expect(result).toEqual({ param2: ['==value'], param3: ['1', '2', '3'], invalidJson: ['{invalid}' ]});
          });
        
        });
        
    })
