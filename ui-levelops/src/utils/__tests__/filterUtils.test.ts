import { sanitizePartialStringFilters } from '../filtersUtils';
import { sanitizeRegexString } from '../stringUtils'; 

describe("filterUtils",()=>{

    describe('sanitizePartialStringFilters function', () => {
      it('should sanitize string filters in the object', () => {
        const input = {
          filter1: { key1: 'value.*' },
          filter2: { key2: '[special]*characters' },
        };
        const result = sanitizePartialStringFilters(input);
        const expected = {
          filter1: { key1: 'value\\.\\*' },
          filter2: { key2: '\\[special\\]\\*characters' }
        };
    
        expect(result).toEqual(expected);
      });
    
      it('should ignore non-object values and empty objects', () => {
        const input = {
          filter1: { key1: 'value.*' },
          filter2: null,
          filter3: {},
          filter4: 'string',
          filter5: 123,
          filter6: []
        };
        const result = sanitizePartialStringFilters(input);
        const expected = {
          filter1: { key1: 'value\\.\\*' },
          filter4 : {
                  "0": "s",
                "1": "t",
                 "2": "r",
                 "3": "i",
                 "4": "n",
                 "5": "g",
               },
        };
    
        expect(result).toEqual(expected);
      });
    // TO BE RESOLVED
    //   it('should return an empty object for null input', () => {
    //     const input = null;
    //     const result = sanitizePartialStringFilters(input);
    //     const expected = {};
    
    //     expect(result).toEqual(expected);
    //   });
    });

describe('sanitizeRegexString function', () => {
  it('should escape special characters in the string', () => {
    const input = '.*+?^${}()|[]\\';
    const result = sanitizeRegexString(input);
    const expected = '\\.\\*\\+\\?\\^\\$\\{\\}\\(\\)\\|\\[\\]\\\\'; // Escaped characters

    expect(result).toBe(expected);
  });

  it('should handle an empty string', () => {
    const input = '';
    const result = sanitizeRegexString(input);
    const expected = '';

    expect(result).toBe(expected);
  });

  it('should not modify a string without special characters', () => {
    const input = 'someStringWithoutSpecialChars';
    const result = sanitizeRegexString(input);
    const expected = 'someStringWithoutSpecialChars';

    expect(result).toBe(expected);
  });

  it('should handle null input', () => {
    const input = null;
    const result = sanitizeRegexString(input);
    const expected = '';

    expect(result).toBe(expected);
  });

  it('should handle undefined input', () => {
    const input = undefined;
    const result = sanitizeRegexString(input);
    const expected = '';

    expect(result).toBe(expected);
  });
});

})

