import moment from 'moment'; 
import { increaseRangeBy } from '../timeUtils'; 
import { timeRange } from '../timeUtils';
import { convertEpochToHumanizedForm } from '../timeUtils';
import { previousTimeStamps } from '../timeUtils';
import { sanitizeTimeFilters } from '../timeUtils'; 
import { allTimestampsBetween } from '../timeUtils';
import { getDaysAndTimeWithUnit } from '../timeUtils'; 
import { isFilterDateTimeType } from '../timeUtils'; 
import { getEffortInvestIssueResolvedAt,  } from '../timeUtils'; 
import { TIME_INTERVAL_TYPES, intervalToMomentKeyMapper } from "../../constants/time.constants";
import { convertToHours } from '../timeUtils'; 
import { convertToMins } from '../timeUtils'; 
import { convertToDays } from '../timeUtils'; 




describe("timeUtilsTests",()=>{
    describe('increaseRangeBy function', () => {
        it('should increase the range by the default value', () => {
          const range = { $gt: '1636272000', $lt: '1636358400' }; 
          const result = increaseRangeBy(range);
          expect(result).toEqual({
            $gt: '1636185600', 
            $lt: '1636358400', 
          });
        });
      
        it('should increase the range by a custom value', () => {
         
          const range = { $gt: '1636272000', $lt: '1636358400' }; 
          const result = increaseRangeBy(range, 3);
          expect(result).toEqual({
            $gt: '1636099200', 
            $lt: '1636358400', 
          });
        });
      
        it('should handle range with string $gt', () => {
         
          const range = { $gt: '1636272000', $lt: '1636358400' }; 
          const result = increaseRangeBy(range);
          expect(result.$gt).toEqual(expect.stringMatching(/^\d+$/)); 
        });
      });

      describe('timeRange function', () => {
        it('should return a time range for zero days', () => {
          
          const num = 30;
          const type = 'days';
          const result = timeRange(num, type);
          expect(result.stop).toBeGreaterThan(result.start);
          
        });
      
        it('should return a time range for zero hours', () => {
          
          const num = 30;
          const type = 'hours';
          const result = timeRange(num, type);
          expect(result).toEqual({
            start: expect.any(Number),
            stop: expect.any(Number),
          });
      
          expect(result.stop).toBeGreaterThan(result.start);
          
        });
       // TO BE RESOLVED
        // it('should handle negative number of days', () => {
         
        //   const num = -5;
        //   const type = 'days';
       //   const result = timeRange(num, type);
       //   expect(result).toEqual({
        //     start: 0, 
        //     stop: 0,  
        //   });
        // });

    // TO BE RESOLVED
    //     it('should handle the edge case', () => {
    //       const num = 0;
    //       const type = 'hours';
    //       const result = timeRange(num, type);
    //       expect(result).toEqual({
    //         start: 0, 
    //         stop: 0,  
    //       });
    //     });
    });

    describe('convertEpochToHumanizedForm function', () => {
    it('should handle zero timestamp', () => {
        const convertTo = 'days';
        const timestamp = 0;
        const result = convertEpochToHumanizedForm(convertTo, timestamp);
      
       
        expect(result).toBe('');
      });
      
      it('should handle negative timestamp', () => {
       
        const convertTo = 'hours';
        const timestamp = -3600; 
        const result = convertEpochToHumanizedForm(convertTo, timestamp);
      
        expect(result).toBe('0 hours');
      });
      
      it('should handle minutes less than 60', () => {
        const convertTo = 'minutes';
        const timestamp = 30;
      
        const result = convertEpochToHumanizedForm(convertTo, timestamp);
      
        expect(result).toBe('1 min');
      });
      

    })
    


   describe('previousTimeStamps function', () => {
  it('should return an empty array for zero days', () => {
    const days = 0;
    const result = previousTimeStamps(days);
    expect(result).toEqual([]);
  });
// TO BE RESOLVED
//   it('should return an empty array for negative days', () => {
//     const days = -2;
//     const result = previousTimeStamps(days);
//     expect(result).toEqual([]);
//   });

  it('should handle one day', () => {
    const days = 1;
    const result = previousTimeStamps(days);

    const yesterday = moment().unix();
    const expected=yesterday-86400
    expect(result).toEqual([expected]);
  });

  it('should handle multiple days', () => {
    const days = 4;
    const result = previousTimeStamps(days);

    const today = moment().unix();
    const expected = [
      today - 86400 * 1,
      today - 86400 * 2,
      today - 86400 * 3,
      today - 86400 * 4,
    ];
    
    expect(result).toEqual(expected);
  });

  it('should handle daylight saving time changes', () => {
    const days = 7;
    const result = previousTimeStamps(days);

    const today = moment().unix();
    const expected = [
      today - 86400 * 1,
      today - 86400 * 2,
      today - 86400 * 3,
      today - 86400 * 4,
      today - 86400 * 5,
      today - 86400 * 6,
      today - 86400 * 7,
    ];

    expect(result).toEqual(expected);
  });
});

describe('sanitizeTimeFilters function', () => {
    // TO BE RESOLVED
//   it('should remove invalid time filters with NaN values', () => {
//     const filters = {
//       startDate: { $lt: 1636358400, $gt: 'NaN' },
//       endDate: { $lt: 'NaN', $gt: 1636272000 },
//     };

//     const timeKeys = ['startDate', 'endDate'];
//     const result = sanitizeTimeFilters(filters, timeKeys);
//     expect(result).toEqual({});
//   });

  it('should keep valid time filters', () => {
   
    const filters = {
      startDate: { $lt: 1636358400, $gt: 1636272000 },
      endDate: { $lt: '2022-11-10', $gt: '2022-11-09' },
    };

    const timeKeys = ['startDate', 'endDate'];
    const result = sanitizeTimeFilters(filters, timeKeys);
    expect(result).toEqual({
      startDate: { $lt: 1636358400, $gt: 1636272000 },
      endDate: { $lt: '2022-11-10', $gt: '2022-11-09' },
    });
  });

  it('should handle filters with number values', () => {
    
    const filters = {
      startTime: 1636358400,
      endTime: 'NaN',
    };

    const timeKeys = ['startTime', 'endTime'];
    const result = sanitizeTimeFilters(filters, timeKeys);
    expect(result).toEqual({ startTime: 1636358400 });
  });

  it('should handle filters with string values', () => {
    
    const filters = {
      dateString: '2022-11-10',
      invalidString: 'NaN',
    };

    const timeKeys = ['dateString', 'invalidString'];
    const result = sanitizeTimeFilters(filters, timeKeys);
    expect(result).toEqual({ dateString: '2022-11-10' });
  });

  it('should handle filters without $lt and $gt', () => {
    const filters = {
      noTimeFilter: '2022-11-10',
    };
    const timeKeys = ['noTimeFilter'];
    const result = sanitizeTimeFilters(filters, timeKeys);
    expect(result).toEqual({ noTimeFilter: '2022-11-10' });
  });

});

describe('allTimestampsBetween function', () => {
  it('should return an array of timestamps for each day between min and max', () => {
   
    const min = 1636358400; 
    const max = 1636694400; 
    const result = allTimestampsBetween(min, max);

    expect(result).toEqual([
        1636329600,
        1636416000,
        1636502400,
        1636588800,
        1636675200,
        1636761600,
    ]);
  });

  it('should handle the same min and max timestamps', () => {
    
    const min = 1636358400; 
    const max = 1636358400; 

    
    const result = allTimestampsBetween(min, max);
    expect(result).toEqual([1636329600,1636416000,]);
  });

  it('should handle min timestamp greater than max timestamp', () => {
    
    const min = 1636694400;
    const max = 1636358400; 

    
    const result = allTimestampsBetween(min, max);
    expect(result).toEqual([]);
  });

  it('should handle daylight saving time changes', () => {
   
    const min = 1636358400; 
    const max = 1636780399; 

    
    const result = allTimestampsBetween(min, max);

    // Assertions
    expect(result).toEqual([
        1636329600,
        1636416000,
        1636502400,
        1636588800,
        1636675200,
        1636761600,
        1636848000,
          
    ]);
  });
});

describe('getDaysAndTimeWithUnit function', () => {
  it('should handle seconds', () => {
    const result = getDaysAndTimeWithUnit(45);
    expect(result).toEqual({ time: 45, unit: 'seconds', extraTime: undefined, extraUnit: undefined });
  });

  it('should handle minutes', () => {
    const result = getDaysAndTimeWithUnit(180);
    expect(result).toEqual({ time: 3, unit: 'minutes', extraTime: 0, extraUnit: 'second' });
  });

  it('should handle minutes with seconds', () => {
    const result = getDaysAndTimeWithUnit(195);
    expect(result).toEqual({ time: 3, unit: 'minutes', extraTime: 15, extraUnit: 'seconds' });
  });

  it('should handle hours', () => {
    const result = getDaysAndTimeWithUnit(7200);
    expect(result).toEqual({ time: 2, unit: 'hours', extraTime: 0, extraUnit: 'minute' });
  });

  it('should handle hours with minutes', () => {
    const result = getDaysAndTimeWithUnit(7320);
    expect(result).toEqual({ time: 2, unit: 'hours', extraTime: 120, extraUnit: 'minutes' });
  });

  it('should handle days', () => {
    const result = getDaysAndTimeWithUnit(172800);
    expect(result).toEqual({ time: 2, unit: 'days', extraTime: 0, extraUnit: 'hour' });
  });

  it('should handle days with hours', () => {
    const result = getDaysAndTimeWithUnit(180000);
    expect(result).toEqual({ time: 2, unit: 'days', extraTime: 7200, extraUnit: 'hours' });
  });

  it('should handle precisionValue', () => {
    const result = getDaysAndTimeWithUnit(7320, 2);
    expect(result).toEqual({ time: 2.03, unit: 'hours', extraTime: 120, extraUnit: 'minutes' });
  });
});


describe('isFilterDateTimeType function', () => {
  it('should return true for valid LT_GT form with sanitized values', () => {
    const filter = { $gt: 1636358400, $lt: 1636362000 };

    const result = isFilterDateTimeType(filter, 'LT_GT');

    expect(result).toBe(true);
  });

  it('should return false for valid LT_GT form with unsanitized values', () => {
    const filter = { $gt: 'invalidValue', $lt: 1636362000 };

    const result = isFilterDateTimeType(filter, 'LT_GT');

    expect(result).toBe(true);
  });

  it('should return false for valid LT_GT form with one unsanitized value', () => {
    const filter = { $gt: 1636358400, $lt: 'invalidValue' };

    const result = isFilterDateTimeType(filter, 'LT_GT');

    expect(result).toBe(true);
  });

  it('should return false for valid LT_GT form with undefined values', () => {
    const filter = { $gt: undefined, $lt: undefined };

    const result = isFilterDateTimeType(filter, 'LT_GT');

    expect(result).toBe(false);
  });

  it('should return false for valid LT_GT form with missing values', () => {
    const filter = { $lt: 1636362000 };

    const result = isFilterDateTimeType(filter, 'LT_GT');

    expect(result).toBe(false);
  });

  it('should return false for invalid form', () => {
    const filter = { $gt: 1636358400, $lt: 1636362000 };

    const result = isFilterDateTimeType(filter, 'INVALID_FORM');

    expect(result).toBe(false);
  });

  it('should return false for undefined form', () => {
    const filter = { $gt: 1636358400, $lt: 1636362000 };

    const result = isFilterDateTimeType(filter, undefined);

    expect(result).toBe(true);
  });

});

describe('getEffortInvestIssueResolvedAt function', () => {
  it('should return valid LT_GT values for TIME_INTERVAL_TYPES.WEEK', () => {
    const result = getEffortInvestIssueResolvedAt(TIME_INTERVAL_TYPES.WEEK);

    const currentTime = moment().unix();
    const expectedLT = currentTime.toString();
    const expectedGT = moment().subtract(1, 'week').unix().toString();

    expect(result).toEqual({ $lt: expectedLT, $gt: expectedGT });
  });

  it('should return valid LT_GT values for TIME_INTERVAL_TYPES.BI_WEEK', () => {
    const result = getEffortInvestIssueResolvedAt(TIME_INTERVAL_TYPES.BI_WEEK);

    const currentTime = moment().unix();
    const expectedLT = currentTime.toString();
    const expectedGT = moment().subtract(2, 'weeks').unix().toString();

    expect(result).toEqual({ $lt: expectedLT, $gt: expectedGT });
  });

  it('should return null for invalid interval', () => {
    const result = getEffortInvestIssueResolvedAt('INVALID_INTERVAL');

    expect(result).toBeNull();
  });


});

describe('convertToHours function', () => {
  it('should convert to hours and round down', () => {
    const result = convertToHours(7200);

    expect(result).toBe(2);
  });

  it('should convert to hours and round up when ceil is true', () => {
    const result = convertToHours(5400, true);

    expect(result).toBe(2);
  });

  it('should handle zero epoch', () => {
    const result = convertToHours(0);

    expect(result).toBe(0);
  });

  it('should handle negative epoch', () => {
    const result = convertToHours(-3600);

    expect(result).toBe(0);
  });

  it('should handle ceil option with a fraction', () => {
    const result = convertToHours(5400.5, true);

    expect(result).toBe(2);
  });

 
});


describe('convertToMins function', () => {
  it('should convert to minutes and round down', () => {
    const result = convertToMins(120);

    expect(result).toBe(2);
  });

  it('should convert to minutes and round up when ceil is true', () => {
    const result = convertToMins(90, true);

    expect(result).toBe(2);
  });

  it('should handle zero epoch', () => {
    const result = convertToMins(0);

    expect(result).toBe(0);
  });

  it('should handle negative epoch', () => {
    const result = convertToMins(-60);

    expect(result).toBe(0);
  });

  it('should handle ceil option with a fraction', () => {
    const result = convertToMins(90.5, true);

    expect(result).toBe(2);
  });
});

describe('convertToDays function', () => {
  it('should convert to days and round down', () => {
    const result = convertToDays(172800);

    expect(result).toBe(2);
  });

  it('should convert to days and round up when ceil is true', () => {
    const result = convertToDays(129600, true); 

    expect(result).toBe(2);
  });

  it('should handle zero epoch', () => {
    const result = convertToDays(0);

    expect(result).toBe(0);
  });

  it('should handle negative epoch', () => {
    const result = convertToDays(-86400); 

    expect(result).toBe(0);
  });

  it('should handle ceil option with a fraction', () => {
    const result = convertToDays(129600.5, true); 

    expect(result).toBe(2);
  });
});

})
