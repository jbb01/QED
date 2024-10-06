package eu.jonahbauer.qed.contact;

import android.accounts.Account;
import android.content.*;
import android.os.Bundle;
import android.os.RemoteException;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.RawContacts;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import eu.jonahbauer.qed.R;
import eu.jonahbauer.qed.model.Person;
import eu.jonahbauer.qed.networking.async.QEDPageReceiver;
import eu.jonahbauer.qed.networking.pages.QEDDBPages;
import eu.jonahbauer.qed.util.Preferences;
import lombok.Value;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.UnaryOperator;

public class ContactSyncAdapter extends AbstractThreadedSyncAdapter {
    private static final String LOG_TAG = ContactSyncAdapter.class.getName();
    public static final Duration UPDATE_INTERVAL = Duration.ofDays(7);

    private final String accountType;
    private final String accountName;

    public ContactSyncAdapter(@NonNull Context context) {
        super(Objects.requireNonNull(context), true);
        this.accountType = context.getString(R.string.account_type);
        this.accountName = context.getString(R.string.app_name);
    }

    @Override
    public void onPerformSync(
            Account account,
            @Nullable Bundle extras,
            @NonNull String authority,
            @NonNull ContentProviderClient provider,
            @NonNull SyncResult syncResult
    ) {
        var manual = extras != null && extras.getBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, false);

        if (manual) {
            Log.i(LOG_TAG, "Performing manual contact synchronization...");
        } else {
            Log.i(LOG_TAG, "Performing contact synchronization...");
        }

        // fetch data
        var favorites = Preferences.getDatabase().getFavorites();
        var contacts = getContactList(provider);
        if (contacts == null) {
            syncResult.databaseError = true;
            return;
        }

        // collect content provider operations for updates, inserts and deletions
        var ops = new ArrayList<ContentProviderOperation>();
        for (var favorite : favorites) {
            var contact = contacts.remove(favorite);
            var person = getPerson(favorite);
            if (person.isEmpty()) {
                // person could not be loaded
                syncResult.stats.numIoExceptions++;
                syncResult.stats.numSkippedEntries++;
            } else if (contact != null) {
                // person is already exported and needs to be updated
                Duration age = Duration.between(contact.getUpdated(), Instant.now());
                if (age.compareTo(UPDATE_INTERVAL) >= 0 || manual) {
                    Log.d(LOG_TAG, "Updating user " + person.get().getFullName() + " (" + person.get().getId() + ")...");
                    syncResult.stats.numUpdates++;
                    syncResult.stats.numEntries++;
                    updatePerson(person.get(), contact.getRawContactId(), ops);
                } else {
                    Log.d(LOG_TAG,
                            "Skipping update of user " + person.get().getFullName() + " (" + person.get().getId() + ")" +
                            " because the last update happened less than " + UPDATE_INTERVAL + " ago (" + age + ")."
                    );
                    syncResult.stats.numSkippedEntries++;
                }
            } else {
                // new person
                Log.d(LOG_TAG, "Inserting user " + person.get().getFullName() + " (" + person.get().getId() + ")...");
                syncResult.stats.numInserts++;
                syncResult.stats.numEntries++;
                insertPerson(person.get(), ops);
            }
        }
        // delete old users
        for (var contact : contacts.values()) {
            Log.d(LOG_TAG, "Deleting user " + contact.getId() + "...");
            syncResult.stats.numDeletes++;
            syncResult.stats.numEntries++;
            deletePerson(contact.getId(), contact.getRawContactId(), ops);
        }

        // execute content provider operations
        try {
            provider.applyBatch(ops);
            Log.i(LOG_TAG, "Contact synchronization finished: " + syncResult.stats);
        } catch (OperationApplicationException | RemoteException ex) {
            syncResult.databaseError = true;
            Log.e(LOG_TAG, "Could not apply content provider operations.", ex);
        }
    }

    private @NonNull Optional<Person> getPerson(long id) {
        var future = new CompletableFuture<Person>();
        var ignored = QEDDBPages.getPerson(new Person(id), QEDPageReceiver.fromFuture(future));
        try {
            return Optional.of(future.get());
        } catch (ExecutionException e) {
            return Optional.empty();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Optional.empty();
        }
    }

    private @Nullable Map<Long, Contact> getContactList(@NonNull ContentProviderClient provider) {
        var projection = new String[] { RawContacts.SOURCE_ID, RawContacts._ID, RawContacts.SYNC1 };
        var selection = RawContacts.ACCOUNT_TYPE + " = ?";
        var arguments = new String[] { accountType };

        try (var cursor = provider.query(RawContacts.CONTENT_URI, projection, selection, arguments, null)) {
            if (cursor == null || !cursor.moveToFirst()) {
                return new HashMap<>();
            }

            var out = new TreeMap<Long, Contact>();
            do {
                long id = cursor.getLong(0);
                long rawContactId = cursor.getLong(1);
                Instant updated = Instant.parse(cursor.getString(2));
                out.put(id, new Contact(id, rawContactId, updated));
            } while (cursor.moveToNext());
            return out;
        } catch (RemoteException ex) {
            Log.e(LOG_TAG, "Error reading contacts.", ex);
            return null;
        }
    }

    private void insertPerson(@NonNull Person person, @NonNull ArrayList<ContentProviderOperation> ops) {
        var idx = ops.size();

        // add account
        ops.add(ContentProviderOperation
                .newInsert(RawContacts.CONTENT_URI.buildUpon().query(ContactsContract.CALLER_IS_SYNCADAPTER).build())
                .withValue(RawContacts.ACCOUNT_TYPE, accountType)
                .withValue(RawContacts.ACCOUNT_NAME, accountName)
                .withValue(RawContacts.SOURCE_ID, String.valueOf(person.getId()))
                .withValue(RawContacts.SYNC1, Objects.requireNonNullElseGet(person.getLoaded(), Instant::now).toString())
                .build());

        // add account data
        insertPersonData(person, ops, builder -> builder.withValueBackReference(Data.RAW_CONTACT_ID, idx));
    }

    private void insertPersonData(
            @NonNull Person person, @NonNull List<ContentProviderOperation> ops,
            @NonNull UnaryOperator<ContentProviderOperation.Builder> withRawContactId
    ) {
        var uri = Data.CONTENT_URI.buildUpon().query(ContactsContract.CALLER_IS_SYNCADAPTER).build();

        // add name
        ops.add(withRawContactId.apply(ContentProviderOperation.newInsert(uri))
                .withValue(Data.MIMETYPE, CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                .withValue(CommonDataKinds.StructuredName.GIVEN_NAME, person.getFirstName())
                .withValue(CommonDataKinds.StructuredName.FAMILY_NAME, person.getLastName())
                .build()
        );

        // add mail
        if (person.getEmail() != null) {
            ops.add(withRawContactId.apply(ContentProviderOperation.newInsert(uri))
                    .withValue(Data.MIMETYPE, CommonDataKinds.Email.CONTENT_ITEM_TYPE)
                    .withValue(CommonDataKinds.Email.ADDRESS, person.getEmail())
                    .build());
        }

        // add birthday
        if (person.getBirthday() != null && person.getBirthday().getLocalDate() != null) {
            ops.add(withRawContactId.apply(ContentProviderOperation.newInsert(uri))
                    .withValue(Data.MIMETYPE, CommonDataKinds.Event.CONTENT_ITEM_TYPE)
                    .withValue(CommonDataKinds.Event.START_DATE, person.getBirthday().getLocalDate().toString())
                    .withValue(CommonDataKinds.Event.TYPE, CommonDataKinds.Event.TYPE_BIRTHDAY)
                    .build()
            );
        }

        // add postal addresses
        for (var address : person.getAddresses()) {
            ops.add(withRawContactId.apply(ContentProviderOperation.newInsert(uri))
                    .withValue(Data.MIMETYPE, CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE)
                    .withValue(CommonDataKinds.StructuredPostal.FORMATTED_ADDRESS, address)
                    .build()
            );
        }

        // add contact details
        for (var contact : person.getContacts()) {
            var op = withRawContactId.apply(ContentProviderOperation.newInsert(uri));
            switch (contact.getType()) {
                case PHONE:
                case PHOEN:
                    ops.add(op
                            .withValue(Data.MIMETYPE, CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                            .withValue(CommonDataKinds.Phone.NUMBER, contact.getValue())
                            .withValue(CommonDataKinds.Phone.TYPE, CommonDataKinds.Phone.TYPE_CUSTOM)
                            .withValue(CommonDataKinds.Phone.LABEL, contact.getLabel())
                            .build()
                    );
                    break;
                case MAIL:
                    ops.add(op
                            .withValue(Data.MIMETYPE, CommonDataKinds.Email.CONTENT_ITEM_TYPE)
                            .withValue(CommonDataKinds.Email.ADDRESS, contact.getValue())
                            .withValue(CommonDataKinds.Email.TYPE, CommonDataKinds.Email.TYPE_CUSTOM)
                            .withValue(CommonDataKinds.Email.LABEL, contact.getLabel())
                            .build()
                    );
                    break;
                default:
                    op.withValue(Data.MIMETYPE, CommonDataKinds.Im.CONTENT_ITEM_TYPE);
                    op.withValue(CommonDataKinds.Im.DATA, contact.getValue());

                    if (contact.getType().getImProtocol() != null) {
                        op.withValue(CommonDataKinds.Im.PROTOCOL, contact.getType().getImProtocol());
                    } else {
                        op.withValue(CommonDataKinds.Im.PROTOCOL, CommonDataKinds.Im.PROTOCOL_CUSTOM);
                        op.withValue(CommonDataKinds.Im.CUSTOM_PROTOCOL, contact.getLabel());
                    }

                    ops.add(op.build());
                    break;
            }
        }
    }

    private void deletePerson(long id, long rawContactId, @NonNull ArrayList<ContentProviderOperation> ops) {
        deletePersonData(rawContactId, ops);
        ops.add(ContentProviderOperation
                .newDelete(RawContacts.CONTENT_URI.buildUpon().query(ContactsContract.CALLER_IS_SYNCADAPTER).build())
                .withSelection(
                        RawContacts.ACCOUNT_TYPE + " = ? AND " + RawContacts.SOURCE_ID + " = ?",
                        new String[] { accountType, String.valueOf(id) }
                )
                .build());
    }

    private void deletePersonData(long rawContactId, @NonNull ArrayList<ContentProviderOperation> ops) {
        ops.add(ContentProviderOperation
                .newDelete(Data.CONTENT_URI.buildUpon().query(ContactsContract.CALLER_IS_SYNCADAPTER).build())
                .withSelection(
                        Data.RAW_CONTACT_ID + " = ?",
                        new String[] { String.valueOf(rawContactId) }
                )
                .build());
    }

    private void updatePerson(@NonNull Person person, long rawContactId, @NonNull ArrayList<ContentProviderOperation> ops) {
        deletePersonData(rawContactId, ops);
        insertPersonData(person, ops, builder -> builder.withValue(Data.RAW_CONTACT_ID, rawContactId));
    }

    @Value
    private static class Contact {
        long id;
        long rawContactId;
        Instant updated;
    }
}
