# Interface-copy review instructions

`interface-copy.csv` is exported directly from `app/src/main/res/values/strings.xml` by `python scripts/export_copy_sheet.py`. It includes every current `<string>` resource, with a stable key, screen context, English source, placeholder list, and blank Tigrinya/reviewer/date/version/result fields. There are no approved translations in this export.

Translate for the actual button, error and feedback context. Keep `%1$s`, `%1$d` and any other numbered placeholders with the same type and number; the order may be adjusted only with numbered indices. Preserve meaning in success/error and development-only labels. Check text length, Ethiopic shaping, punctuation, mixed Arabic/Latin direction and TalkBack speech on a device. The current resources have no `<plurals>` entries; if a plural is introduced, review each quantity form separately and extend the exporter before treating this sheet as complete. Record the reviewed app/content version and corrections, then integrate only supplied, approved wording in a later content/localization task.

The sheet's context labels are editorial navigation aids generated from key groups; the English and keys are copied from actual resources. Reviewers should inspect the running app and the [pilot checklist](PHYSICAL_DEVICE_CHECKLIST.md) for final context.
