import {DateFormats, convertEpochToDate, getDateFromTimeStampInGMTFormat} from "../dateUtils"
import { getEndValue } from '../dateUtils'; 
import moment,{Moment} from 'moment';
import { toBeChecked } from "@testing-library/jest-dom/matchers";
import { getStartOfDayFromDate } from "../dateUtils";
import { getStartOfDayFromDateString } from '../dateUtils'; 
import { getStartOfDayTimeStamp } from '../dateUtils'; 
import { getStartValue } from '../dateUtils'; 

describe('getStartValue', () => {
  it('returns the first valid moment in the array', () => {
    const Moment1 = moment('2023-10-31');
    const Moment2 = moment('2023-11-01');
    const invalidMoment = moment.invalid();

    const valueArray = [Moment1, invalidMoment, Moment2];
    const result = getStartValue(valueArray);

    expect(result).toBe(Moment1); 
  });

  it('returns undefined for an empty array', () => {
    const valueArray = [];
    const result = getStartValue(valueArray);

    expect(result).toBeUndefined();
  });

  it('should return undefined for an array of invalid moments', () => {
    const invalidMoment1 = moment.invalid();
    const invalidMoment2 = moment.invalid();

    const valueArray = [invalidMoment1, invalidMoment2];
    const result = getStartValue(valueArray);

    expect(result).toBeUndefined();
  });

  // it('should return undefined for an array of null', () => {
  //   const valueArray = [null, null, null];
  //   const result = getStartValue(valueArray);

  //   expect(result).toBeUndefined();
  // });
});

describe('getStartOfDayTimeStamp', () => {
  
  it("epoch is empty",()=>{
    const expectedTimestamp=getStartOfDayTimeStamp('');
    expect(expectedTimestamp).toBe(0)
  }) 

  it('returns the start of the day in Unix timestamp format', () => {
    const epoch = 1635638400; 
    const unixTimestamp = getStartOfDayTimeStamp(epoch);

    const date = moment.unix(epoch);
    const expectedTimestamp = date.startOf('day').unix();

    expect(unixTimestamp).toBe(expectedTimestamp);
  });

  it('returns the start of the day in Unix timestamp format without converting', () => {
    const epoch = 1635638400; 
    const unixTimestamp = getStartOfDayTimeStamp(epoch, false);

    
    const date = moment.unix(epoch);
    const expectedTimestamp = date.startOf('day').unix();

    expect(unixTimestamp).toBe(expectedTimestamp);
  });
});


describe('getStartOfDayFromDateString', () => {
  it('String is empty', () => {
    expect(getStartOfDayFromDateString('')).toBe(0);
  });

  it('returns the start of the day in Unix timestamp format for the default format', () => {
    const date = '2023/10/31'; 
    const unixTimestamp = getStartOfDayFromDateString(date);

    const expectedTimestamp = moment.utc(date, 'YYYY/MM/DD').startOf('day').unix();

    expect(unixTimestamp).toBe(expectedTimestamp);
  });

  it('returns the start of the day in Unix timestamp format', () => {
    const date = '31-10-2023';
    const format = 'DD-MM-YYYY'; 
    const unixTimestamp = getStartOfDayFromDateString(date, format);

    const expectedTimestamp = moment.utc(date, format).startOf('day').unix();

    expect(unixTimestamp).toBe(expectedTimestamp);
  });
});


describe("getStartOfDayFromDate",()=>{
    it("date is undefined",()=>{
        const date=undefined;
        const convert=true;
        const result=getStartOfDayFromDate(date,convert)
        expect(result).toBe(0)
    })
    it("date is NULL",()=>{
        const date=null;
        const convert=true;
        const result=getStartOfDayFromDate(date,convert);
        expect(result).toBe(0);
    })

    it('returns the start of the day in Unix timestamp format', () => {
    const date = moment('2023-10-31'); 
    const unixTimestamp = getStartOfDayFromDate(date);

    const expectedTimestamp = date.startOf('day').unix();

    expect(unixTimestamp).toBe(expectedTimestamp);
  });

  it('returns the start of the day in Unix timestamp format without converting', () => {
    const date = moment('31-10-2023'); 
    const unixTimestamp = getStartOfDayFromDate(date, false);

    const expectedTimestamp = date.startOf('day').unix();

    expect(unixTimestamp).toBe(expectedTimestamp);
  });

});



describe('getEndValue', () => {
  it('returns the second valid moment in the array', () => {
    const Moment1 = moment('2023-10-31');
    const Moment2 = moment('2023-11-01');
    const invalidMoment = moment.invalid();

    const valueArray = [Moment1, Moment2,invalidMoment];
    const result = getEndValue(valueArray);

    expect(result).toBe(Moment2);
  });

  it('returns undefined for an empty array', () => {
    const valueArray = [];
    const result = getEndValue(valueArray);

    expect(result).toBeUndefined();
  });

  it('returns undefined for an array with only one valid moment', () => {
    const Moment1 = moment('2023-10-31');
    const valueArray = [Moment1];
    const result = getEndValue(valueArray);

    expect(result).toBeUndefined();
  });

  it('returns undefined for an array of invalid moments', () => {
    const invalidMoment1 = moment.invalid();
    const invalidMoment2 = moment.invalid();

    const valueArray = [invalidMoment1, invalidMoment2];
    const result = getEndValue(valueArray);

    expect(result).toBeUndefined();
  });

  // TODO: to be resolved later
//   it('returns undefined for an array of null', () => {
//     const valueArray = [null, null, null];
//     const result = getEndValue(valueArray);

//     expect(result).toBeUndefined();
//   });
});



describe("convertEpochToDate",()=>{
  

  it("UTC is flase",()=>{
    const epoch=1698732452;
    const format="DD MM YYYY";
    const utc=false;
    const result=convertEpochToDate(epoch,format,utc)
    expect(result).toEqual('31 10 2023')
  })
  
  it("UTC is true",()=>{
    const epoch=1698732452;
   const format='DD MMM YYYY'
   const result=convertEpochToDate(epoch,format)
   expect(result).toEqual('31 Oct 2023')
  })

  it("UTC is true and format is Quater Month",()=>{
    const epoch=1698732452;
   const newEpoch=getDateFromTimeStampInGMTFormat(1698732452);
   const format=DateFormats.QUARTER_MONTH;
   const result=convertEpochToDate(epoch,format)
   expect(result).toEqual('4-2023')

  })
  
  it("UTC is true and format is week",()=>{
    const epoch=1698732452;
   const newEpoch=getDateFromTimeStampInGMTFormat(1698732452);
   const format=DateFormats.WEEK;
   const result=convertEpochToDate(epoch,format)
   expect(result).toEqual('44-2023')

  })
  it("UTC is true and format is ISO week",()=>{
    const epoch=1698732452;
    const newEpoch=getDateFromTimeStampInGMTFormat(1698732452);
    const format=DateFormats.ISO_WEEK;
    const result=convertEpochToDate(epoch,format)
    expect(result).toEqual('44-2023')
  })

  

});


  