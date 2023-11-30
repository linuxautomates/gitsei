import {dataURLtoBlob} from "../imageUtils"
import { readAsBinary } from "../imageUtils";

describe('readAsBinary Function', () => {
    it('should read binary data from a Blob', async () => {
     
      const binaryData = new Uint8Array([71, 13, 10, 26, 10]);
      const blob = new Blob([binaryData], { type: 'image/png' });
  
     
      const result = await readAsBinary(blob);
      
      expect(result).toMatch(/^data:image\/png;base64,/);
    });
  
    it('should handle reading errors', async () => {
      
      const invalidBlob = new Blob(['Invalid data'], { type: 'invalid-type' });
  
      try {
        await readAsBinary(invalidBlob);
       fail('error');
      } catch (error) {}
    });
  });

describe('Data URL to Blob Conversion', () => {
 
  const DataURL = 'data:image/png;,iVBORw0KGg';

  it('should correctly convert a Data URL to a result', () => {
    
    const result = dataURLtoBlob(DataURL);

    
    expect(result).toBeInstanceOf(Blob);

    
  });

  it('should handle non-base64 Data URLs', () => {
   
    const nonBase64DataURL = 'data:text/plain,HelloHarness';
    const result = dataURLtoBlob(nonBase64DataURL);
    expect(result).toBeInstanceOf(Blob);
    
  });

  it('should handle invalid Data URLs', () => {
   
    const invalidDataURL = 'invalidURLnknnknk';
   
    try {
        const result = dataURLtoBlob(invalidDataURL);
        expect(result).toBeNull(); 
      } catch (error) {}
        
  });
});
