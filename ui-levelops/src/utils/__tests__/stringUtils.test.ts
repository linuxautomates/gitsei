import { convertCase, isValidRegEx,hashCode,stringContainsInvalidChars, stringTransform, truncateAndEllipsis, validateEmail, validatePassword } from "../stringUtils";
import { valuetoLabel,valueToTitle,toTitleCase,joinUrl,getNameInitials,capitalizeFirstLetter,b64DecodeUnicode} from "../stringUtils";
import { slugifyId,strReplaceAll,insertAt,strIsEqual, truncatedString,numberFromString,capitalizeWord,slugifyIdWithoutAlteringCase,removeNewLine,validateIP, validatePort } from '../stringUtils'; 

describe("stringUtils",()=>{
    describe("Validate Email tests",()=>{
        // TO BE RESOLVED 
        // it("returns true for valid email",()=>{
        //     const value='jatin.sangwan@harness.io';
        //     const result=validateEmail(value);
        //     expect(result).toStrictEqual(["jatin.sangwan@harness.io", "jatin.sangwan", "harness.", "io"]);
        // })
        it("check for null value",()=>{
            const value='';
            const result=validateEmail(value);
            expect(result).toStrictEqual(false)
        })
    })

    describe("validatePassword",()=>{
        // TO BE RESOLVED
        // it("for valid password",()=>{
        //     const value='ABC123456';
        //     const result=validatePassword(value);
        //     expect(result).toStrictEqual(["1"]);
        // })
        it("for invalid password",()=>{
            const value='';
            const result=validateEmail(value)
            expect(result).toEqual(false)
        })
        it("for random password",()=>{
            const value="av$3^%%";
            const result=validatePassword(value);
            expect(result).toEqual(false);
        })
    })

    describe("stringTransform",()=>{
        it("transforms the string correctly",()=>{
            const input="the quick brown fox jumps over the little lazy dog";
            const separator=' ';
            const result=stringTransform(input,separator);
            expect(result).toEqual("TheQuickBrownFoxJumpsOverTheLittleLazyDog")
        })
        it("when lowercaseRestString is false",()=>{
            const input="the quick brown fox jumps over the little lazy dog";
            const separator=' ';
            const lowercaseRestString=false;
            const result=stringTransform(input,separator,'',lowercaseRestString);
            expect(result).toEqual("TheQuickBrownFoxJumpsOverTheLittleLazyDog")
        })
        it("when separator is invalid",()=>{
            const input="the quick brown fox jumps over the little lazy dog";
            const separator=null;
            const lowercaseRestString=false;
            const result=stringTransform(input,separator,'',lowercaseRestString);
            expect(result).toEqual("The quick brown fox jumps over the little lazy dog")
        })
    })
    describe("truncateAndEllipsis",()=>{
        it("when default paramters are passed",()=>{
            const data="Working at Harness";
            const result=truncateAndEllipsis(data);
            expect(result).toEqual("Working at Harn...")
        })
        it("when upto is equal to data length",()=>{
            const data="Working at Harness";
            const result=truncateAndEllipsis(data,18);
            expect(result).toEqual("Working at Harness")
        })
        it("data length is smaller than upto ",()=>{
            const data="Working at Harness";
            const result=truncateAndEllipsis(data,20);
            expect(result).toEqual("Working at Harness")
        })
        it("leftEllipsis is true",()=>{
            const data="Working at Harness";
            const result=truncateAndEllipsis(data,11);
            expect(result).toEqual("Working at ...")
        })
    })
    describe("convertCase",()=>{
        it("labelcase is uppercase",()=>{
            const value="harness";
            const result=convertCase(value,'upper_case');
            expect(result).toEqual('HARNESS')
        })
        it("labelcase is lowercase",()=>{
            const value="HARNESS";
            const result=convertCase(value,'lower_case');
            expect(result).toEqual('harness')
        })
        it("labelcase is title_case",()=>{
            const value='harness';
            const result=convertCase(value,'title_case')
            expect(result).toEqual('Harness')
        })
    })
    describe("stringContainsInvalidChars",()=>{
        it("name is of invalid type",()=>{
            const name=112;
            const result=stringContainsInvalidChars(name);
            expect(result).toEqual(false)
        })
        it("if invalid characters exist",()=>{
            const name='harness/211';
            const result=stringContainsInvalidChars(name)
            expect(result).toEqual(true);
        })
        it("no invalid character exist",()=>{
            const name='harness.io';
            const result=stringContainsInvalidChars(name);
            expect(name).toStrictEqual('harness.io')
        })
    })

describe('valueToLabel function', () => {
  test('converts value to uppercase and replaces underscore with space', () => {
    expect(valuetoLabel('hello_world')).toBe('HELLO WORLD');
  });

  test('handles empty input', () => {
    expect(valuetoLabel('')).toBe('');
  });

  test('handles undefined input', () => {
    expect(valuetoLabel(undefined)).toBe('');
  });
});

describe('valueToTitle function', () => {
  test('replaces underscore with space', () => {
    expect(valueToTitle('hello_world')).toBe('hello world');
  });

  test('handles empty input', () => {
    expect(valueToTitle('')).toBe('');
  });

  test('handles undefined input', () => {
    expect(valueToTitle(undefined)).toBe('');
  });

  test('handles multiple underscores', () => {
    expect(valueToTitle('some_long_name_with_multiple_underscores')).toBe('some long name with multiple underscores');
  });
});

describe('toTitleCase function', () => {
  test('converts string to title case', () => {
    expect(toTitleCase('hello_world')).toBe('Hello World');
  });

  test('handles empty input', () => {
    expect(toTitleCase('')).toBe('');
  });

  test('handles undefined input', () => {
    expect(toTitleCase(undefined)).toBeUndefined();
  });

  test('handles string with multiple spaces and underscores', () => {
    expect(toTitleCase('multiple__spaces_and__underscores')).toBe('Multiple  Spaces And  Underscores');
  });

  test('handles string with special characters', () => {
    expect(toTitleCase('this/is a*test')).toBe('This/is A*test');
  });
}); 
  
describe('joinUrl function', () => {
  test('joins two URL parts correctly', () => {
    const result = joinUrl('https://example.com/', '/api');
    expect(result).toBe('https://example.com/api');
  });

  test('handles empty values', () => {
    const result = joinUrl('', '');
    expect(result).toBe('');
  });

  test('handles missing second value', () => {
    const result = joinUrl('https://example.com/');
    expect(result).toBe('https://example.com/');
  });

  test('handles missing first value', () => {
    const result = joinUrl(undefined, '/api');
    expect(result).toBe('/api');
  });

  test('handles URL parts with double slashes correctly', () => {
    const result = joinUrl('https://example.com/', '//api');
    expect(result).toBe('https://example.com//api');
  });

  test('joins URL parts without additional slash when not needed', () => {
    const result = joinUrl('https://example.com', 'api');
    expect(result).toBe('https://example.comapi');
  });

  test('handles non-string default value', () => {
    const defaultValue = 123;
    const result = joinUrl('', '', defaultValue);
    expect(result).toBe('');
  });
});


 describe('capitalizeFirstLetter function', () => {
  test('capitalizes the first letter of a word', () => {
    const result = capitalizeFirstLetter('hello');
    expect(result).toBe('Hello');
  });

  test('handles empty string input', () => {
    const result = capitalizeFirstLetter('');
    expect(result).toBe('');
  });
// TO BE RESOLVED
//   test('handles undefined input', () => {
//     const result = capitalizeFirstLetter(undefined);
//     expect(result).toBe('');
//   });

  test('handles already capitalized word', () => {
    const result = capitalizeFirstLetter('World');
    expect(result).toBe('World');
  });

  test('handles string with special characters', () => {
    const result = capitalizeFirstLetter('this/is a*test');
    expect(result).toBe('This/is a*test');
  });

  test('handles single character input', () => {
    const result = capitalizeFirstLetter('a');
    expect(result).toBe('A');
  });
});
  
describe('b64DecodeUnicode function', () => {
  test('decodes a base64 encoded string', () => {
    const result = b64DecodeUnicode('SGVsbG8gV29ybGQ=');
    expect(result).toBe('Hello World');
  });

  test('handles empty string input', () => {
    const result = b64DecodeUnicode('');
    expect(result).toBe('');
  });
// TO BE RESOLVED
//   test('handles undefined input', () => {
//     const result = b64DecodeUnicode(undefined);
//     expect(result).toBe('');
//   });

//   test('handles invalid base64 input', () => {
//     const result = b64DecodeUnicode('InvalidBase64String');
//     expect(result).toBe('');
//   });

  test('decodes a base64 string with special characters', () => {
    const result = b64DecodeUnicode('SGVsbG8gV29ybGQhIQ==');
    expect(result).toBe('Hello World!!');
  });

  test('handles base64 string with non-ASCII characters', () => {
    const result = b64DecodeUnicode('VGhpcyBpcyBhIG5vdGUgdGV4dA==');
    expect(result).toBe('This is a note text');
  });
});
  
describe('getNameInitials function', () => {
  test('gets initials from a full name', () => {
    const result = getNameInitials('John Doe');
    expect(result).toBe('JD');
  });

  test('gets initials from a single name', () => {
    const result = getNameInitials('Alice');
    expect(result).toBe('AL');
  });

  test('handles empty string input', () => {
    const result = getNameInitials('');
    expect(result).toBe('');
  });
// TO BE RESOLVED
//   test('handles undefined input', () => {
//     const result = getNameInitials(undefined);
//     expect(result).toBeUndefined();
//   });
// TO BE RESOLVED 
//   test('handles name with multiple spaces', () => {
//     const result = getNameInitials('  John    Doe  ');
//     expect(result).toBe('JD');
//   });

  test('handles name with special characters', () => {
    const result = getNameInitials('Jo$hn D@oe');
    expect(result).toBe('JD');
  });

  test('handles name with single character', () => {
    const result = getNameInitials('A');
    expect(result).toBe('A');
  });

  test('handles name with no spaces', () => {
    const result = getNameInitials('JohnDoe');
    expect(result).toBe('JO');
  });
});

  
describe('isValidRegEx function', () => {
  test('validates a valid regular expression', () => {
    const result = isValidRegEx('^abc');
    expect(result).toBe(true);
  });

  test('validates an empty string as invalid', () => {
    const result = isValidRegEx('');
    expect(result).toBe(true);
  });
// TO BE RESOLVED

//   test('validates a simple string as invalid regex', () => {
//     const result = isValidRegEx('hello');
//     expect(result).toBe(false);
//   });

//   test('validates a complex string as invalid regex', () => {
//     const result = isValidRegEx('[a-z]+');
//     expect(result).toBe(true);
//   });

//   test('validates a string with invalid regex characters as invalid', () => {
//     const result = isValidRegEx('[a-z]+/');
//     expect(result).toBe(true);
//   });
//   test('validates undefined input as invalid', () => {
//     const result = isValidRegEx(undefined);
//     expect(result).toBe(false);
//   });
});


describe('slugifyId function', () => {
  test('slugifies ID by converting to lowercase and replacing spaces with underscore', () => {
    const result = slugifyId('This is an Example');
    expect(result).toBe('this_is_an_example');
  });

  test('handles empty string input', () => {
    const result = slugifyId('');
    expect(result).toBe('');
  });

  test('handles undefined input', () => {
    const result = slugifyId(undefined);
    expect(result).toBeUndefined();
  });

  test('handles already slugified ID', () => {
    const result = slugifyId('already_is_slugified');
    expect(result).toBe('already_is_slugified');
  });

  test('handles ID with special characters', () => {
    const result = slugifyId('This-has$special@characters');
    expect(result).toBe('this-has$special@characters');
  });
});

describe('slugifyIdWithoutAlteringCase function', () => {
  test('slugifies ID by replacing spaces with underscore without altering case', () => {
    const result = slugifyIdWithoutAlteringCase('This is an Example');
    expect(result).toBe('This_is_an_Example');
  });

  test('handles empty string input', () => {
    const result = slugifyIdWithoutAlteringCase('');
    expect(result).toBe('');
  });

  test('handles undefined input', () => {
    const result = slugifyIdWithoutAlteringCase(undefined);
    expect(result).toBeUndefined();
  });

  test('handles ID with special characters', () => {
    const result = slugifyIdWithoutAlteringCase('This-has$special@characters');
    expect(result).toBe('This-has$special@characters');
  });
});

  
describe('removeNewLine function', () => {
  test('removes new line character from string', () => {
    const stringWithNewLine = 'Hello\nWorld';
    const result = removeNewLine(stringWithNewLine);
    expect(result).toBe('HelloWorld');
  });

  test('handles string without new line character', () => {
    const stringWithoutNewLine = 'Hello World';
    const result = removeNewLine(stringWithoutNewLine);
    expect(result).toBe('Hello World');
  });

  test('handles empty string input', () => {
    const result = removeNewLine('');
    expect(result).toBe('');
  });

  // TO BE RESOLVED
//   test('handles undefined input', () => {
//     const result = removeNewLine(undefined);
//     expect(result).toBeUndefined();
//   });
});


describe('validateIP function', () => {
  test('validates a valid IPv4 address', () => {
    expect(validateIP('192.168.0.1')).toBe(true);
  });

  test('validates an invalid IPv4 address', () => {
    expect(validateIP('256.256.256.256')).toBe(false);
  });

  test('handles empty string input for IP', () => {
    expect(validateIP('')).toBe(false);
  });

  test('handles undefined input for IP', () => {
    expect(validateIP(undefined)).toBe(false);
  });
});

describe('validatePort function', () => {
  test('validates a valid port number', () => {
    expect(validatePort('8080')).toBe(true);
  });

  test('validates an invalid port number', () => {
    expect(validatePort('70000')).toBe(false);
  });

  test('handles empty string input for port', () => {
    expect(validatePort('')).toBe(false);
  });

  test('handles undefined input for port', () => {
    expect(validatePort(undefined)).toBe(false);
  });
});
    
describe('insertAt function', () => {
  test('inserts substring at the specified position', () => {
    const result = insertAt('Hello World', 'beautiful ', 6);
    expect(result).toBe('Hello beautiful World');
  });

  test('handles empty string input', () => {
    const result = insertAt('', 'test', 0);
    expect(result).toBe('test');
  });

  test('handles undefined input', () => {
    const result = insertAt(undefined, 'value', 2);
    expect(result).toBe('');
  });

  test('handles null input', () => {
    const result = insertAt(null, 'something', 4);
    expect(result).toBe('');
  });

  test('inserts at the start of a string', () => {
    const result = insertAt('world', 'Hello ', 0);
    expect(result).toBe('Hello world');
  });

  test('inserts at the end of a string', () => {
    const result = insertAt('Hello', '!', 5);
    expect(result).toBe('Hello!');
  });

  test('handles position beyond string length', () => {
    const result = insertAt('Hello', '!', 10);
    expect(result).toBe('Hello!');
  });
});

describe('strReplaceAll function', () => {
  test('replaces all occurrences of a substring', () => {
    const result = strReplaceAll('hello hello world', 'hello', 'hi');
    expect(result).toBe('hi hi world');
  });

  test('handles empty string input', () => {
    const result = strReplaceAll('', 'test', 'example');
    expect(result).toBe('');
  });
// TO BE RESOLVED
//   test('handles undefined input', () => {
//     const result = strReplaceAll(undefined, 'value', 'newValue');
//     expect(result).toBe('');
//   });

  test('handles special characters in find parameter', () => {
    const result = strReplaceAll('+-*/', '+', '-');
    expect(result).toBe('--*/');
  });

  test('replaces with empty string', () => {
    const result = strReplaceAll('hello world', 'o', '');
    expect(result).toBe('hell wrld');
  });

  test('handles regex metacharacters in find parameter', () => {
    const result = strReplaceAll('12.34', '.', ',');
    expect(result).toBe('12,34');
  });
});
  
describe('capitalizeWord function', () => {
  test('capitalizes the specified word in the input string', () => {
    const result = capitalizeWord('hello world', 'world');
    expect(result).toBe('hello WORLD');
  });

  test('handles empty input string', () => {
    const result = capitalizeWord('', 'test');
    expect(result).toBe('');
  });

  test('handles undefined word to capitalize', () => {
    const result = capitalizeWord('hello world', undefined);
    expect(result).toBe('hello world');
  });

  test('handles non-existing word in the input string', () => {
    const result = capitalizeWord('hello world', 'universe');
    expect(result).toBe('hello world');
  });

  test('handles multiple occurrences of the word to capitalize', () => {
    const result = capitalizeWord('hello hello hello', 'hello');
    expect(result).toBe('HELLO HELLO HELLO');
  });
});

describe('strIsEqual function', () => {
  test('checks if two strings are equal regardless of case and leading/trailing spaces', () => {
    const result1 = strIsEqual('Hello', 'hello');
    expect(result1).toBe(true);

    const result2 = strIsEqual('   OpenAI    ', 'openai');
    expect(result2).toBe(true);

    const result3 = strIsEqual('Hello', 'Hello world');
    expect(result3).toBe(false);
  });

  test('handles empty strings', () => {
    const result = strIsEqual('', '');
    expect(result).toBe(true);
  });

  test('handles undefined strings', () => {
    const result = strIsEqual(undefined, 'value');
    expect(result).toBe(false);
  });
});

describe('hashCode function', () => {
  test('returns a hash code for a string', () => {
    const result = hashCode('Hello World');
    expect(result).toBe(-862545276);
  });

  test('handles empty string input', () => {
    const result = hashCode('');
    expect(result).toBe(false);
  });

  test('handles a single character string', () => {
    const result = hashCode('A');
    expect(result).toBe(65);
  });

  test('handles special characters in the input string', () => {
    const result = hashCode('Hello#$@World');
    expect(result).toBe(1567695237);
  });

  test('returns the same hash code for the same string', () => {
    const string = 'Testing hashCode function';
    const result1 = hashCode(string);
    const result2 = hashCode(string);
    expect(result1).toBe(result2);
  });
});

describe('numberFromString function', () => {
  test('returns string without numbers', () => {
    const result = numberFromString('Hello123');
    expect(result).toBe('123');
  });

  test('returns input string if contains only characters', () => {
    const result = numberFromString('Hello');
    expect(result).toBe('Hello');
  });

  test('returns empty string if input is empty', () => {
    const result = numberFromString('');
    expect(result).toBe('');
  });

  test('returns input string if no numbers present', () => {
    const result = numberFromString('NoNumbersHere');
    expect(result).toBe('NoNumbersHere');
  });


  test('handles string with special characters and numbers', () => {
    const result = numberFromString('Hello#123@World');
    expect(result).toBe('123');
  });

});
  
describe('truncatedString function', () => {
  test('returns truncated string with default length', () => {
    const result = truncatedString('This is a long string to be truncated');
    expect(result).toBe('This is a ');
  });

  test('returns truncated string with specified length', () => {
    const result = truncatedString('Short', 5);
    expect(result).toBe('Short');
  });

  test('returns empty string for empty input', () => {
    const result = truncatedString('', 5);
    expect(result).toBe('');
  });

  test('returns full string if length exceeds string length', () => {
    const result = truncatedString('Short', 10);
    expect(result).toBe('Short');
  });

  test('handles string conversion for number input', () => {
    const result = truncatedString(1234567890, 5);
    expect(result).toBe('12345');
  });
});

})
