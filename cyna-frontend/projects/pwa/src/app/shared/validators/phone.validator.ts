import { AbstractControl, ValidationErrors } from '@angular/forms';
import { parsePhoneNumberFromString } from 'libphonenumber-js/min';

export function phoneValidator(getCountry: () => string | null) {
  return (control: AbstractControl): ValidationErrors | null => {
    const value = control.value;
    const country = getCountry();

    if (!value || !country) return null;

    try {
      const phone = parsePhoneNumberFromString(value, country as any);

      if (!phone || !phone.isValid()) {
        return { phoneInvalid: true };
      }

      return null;
    } catch {
      return { phoneInvalid: true };
    }
  };
}
