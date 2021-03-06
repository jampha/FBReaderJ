/*
 * Copyright (C) 2010-2012 Geometer Plus <contact@geometerplus.com>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301, USA.
 */

package org.geometerplus.android.fbreader.library;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.benetech.android.R;
import org.geometerplus.android.fbreader.FBReader;
import org.geometerplus.android.fbreader.benetech.FBReaderWithNavigationBar;
import org.geometerplus.android.fbreader.network.bookshare.BookDetailActivity;
import org.geometerplus.fbreader.library.Author;
import org.geometerplus.fbreader.library.Book;
import org.geometerplus.fbreader.library.Library;
import org.geometerplus.fbreader.library.SeriesInfo;
import org.geometerplus.fbreader.library.Tag;
import org.geometerplus.fbreader.network.HtmlUtil;
import org.geometerplus.zlibrary.core.filesystem.ZLFile;
import org.geometerplus.zlibrary.core.image.ZLImage;
import org.geometerplus.zlibrary.core.image.ZLLoadableImage;
import org.geometerplus.zlibrary.core.language.ZLLanguageUtil;
import org.geometerplus.zlibrary.core.resources.ZLResource;
import org.geometerplus.zlibrary.ui.android.image.ZLAndroidImageData;
import org.geometerplus.zlibrary.ui.android.image.ZLAndroidImageManager;
import org.geometerplus.zlibrary.ui.android.util.SortUtil;

import java.io.File;
import java.text.DateFormat;
import java.util.Date;
import java.util.HashSet;

public class DownloadedBookInfoActivity extends BookDetailActivity {
	public static final int REQUEST_BOOK_INFO = 1001;
	public static final int RESULT_BOOK_DELETED = 100;
	private static final boolean ENABLE_EXTENDED_FILE_INFO = false;

	public static final String CURRENT_BOOK_PATH_KEY = "CurrentBookPath";
	public static final String FROM_READING_MODE_KEY = "fromReadingMode";

	private final ZLResource myResource = ZLResource.resource("bookInfo");
	private ZLFile myFile;
	private ZLImage myImage;
	private boolean myDontReloadBook;
	private boolean isBookshareIdAvailable;

    //Added for the detecting whether the talkback is on
    private AccessibilityManager accessibilityManager;

	@Override
	protected void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		Thread.setDefaultUncaughtExceptionHandler(
			new org.geometerplus.zlibrary.ui.android.library.UncaughtExceptionHandler(this)
		);

		final String path = getIntent().getStringExtra(CURRENT_BOOK_PATH_KEY);
		myDontReloadBook = getIntent().getBooleanExtra(FROM_READING_MODE_KEY, false);
		myFile = ZLFile.createFileByPath(path);

		myImage = Library.getCover(myFile);

		if (SQLiteBooksDatabase.Instance() == null) {
			new SQLiteBooksDatabase(this);
		}
		setContentView(R.layout.book_info);

		setResult(1, getIntent());

        accessibilityManager =
            (AccessibilityManager) getApplicationContext().getSystemService(Context.ACCESSIBILITY_SERVICE);

		ViewGroup rootLayout = (ViewGroup)findViewById(R.id.book_info_root);
		SortUtil.applyCurrentFontToAllInViewGroup(this, rootLayout);

		btnReadingList = findButton(R.id.bookshare_btn_readinglist_bottom);

	}

	@Override
	protected boolean shouldShowAddToReadingListButton(){
		if(isBookshareIdAvailable) return super.shouldShowAddToReadingListButton();
		else return false;
	}


	@Override
	protected void onStart() {
		final Book book = Book.getByFile(myFile);
		isBookshareIdAvailable = book.getBookshareId() != 0;
		super.onStart();
		if (book != null) {
			setupCover(book);
			setupBookInfo(book);
			setupAnnotation(book);
			setupFileInfo(book);
		}


		btnReadingList.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				showReadingListsDialog(Long.toString(book.getBookshareId()));
			}
		});

		setupButton(R.id.book_info_button_open, "openBook", new View.OnClickListener() {
			public void onClick(View view) {
				if (myDontReloadBook) {
					finish();
				} else {
					startActivity(
						new Intent(getApplicationContext(), FBReaderWithNavigationBar.class)
							.setAction(Intent.ACTION_VIEW)
							.putExtra(FBReader.BOOK_PATH_KEY, myFile.getPath())
							.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
					);
				}
			}
		});
		setupButton(R.id.book_info_button_remove, "removeBook", new View.OnClickListener() {
			public void onClick(View view) {
				if (book != null) {
					deleteBook(book);
				}
			}
		});




		final View root = findViewById(R.id.book_info_root);
		root.invalidate();
		root.requestLayout();


	}


	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if(requestCode == START_READINGLIST_DIALOG) {
			super.onActivityResult(requestCode, resultCode, data);
		}
		else {
			final Book book = Book.getByFile(myFile);
			if (book != null) {
				setupBookInfo(book);
				myDontReloadBook = false;
			}
		}
	}

	private Button findButton(int buttonId) {
		return (Button)findViewById(buttonId);
	}

	private void setupButton(int buttonId, String resourceKey, View.OnClickListener listener) {
		final ZLResource buttonResource = ZLResource.resource("dialog").getResource("button");
		final Button button = findButton(buttonId);
		button.setText(buttonResource.getResource(resourceKey).getValue());
		button.setOnClickListener(listener);
	}

	private void setupInfoPair(int id, String key, CharSequence value) {
		final LinearLayout layout = (LinearLayout)findViewById(id);
		if (value == null || value.length() == 0) {
			layout.setVisibility(View.GONE);
			return;
		}
		layout.setVisibility(View.VISIBLE);
		((TextView)layout.findViewById(R.id.book_info_key)).setText(myResource.getResource(key).getValue());
		((TextView)layout.findViewById(R.id.book_info_value)).setText(value);
	}

	private void setupCover(Book book) {
		final ImageView coverView = (ImageView)findViewById(R.id.book_cover);

		setCover(getWindowManager(), coverView, myImage);
	}

	public static void setCover(WindowManager windowManager, ImageView coverView, ZLImage imageToUse) {
		coverView.setVisibility(View.GONE);
		coverView.setImageDrawable(null);

		if (imageToUse == null) {
			return;
		}

		if (imageToUse instanceof ZLLoadableImage) {
			final ZLLoadableImage loadableImage = (ZLLoadableImage) imageToUse;
			if (!loadableImage.isSynchronized()) {
				loadableImage.synchronize();
			}
		}
		final ZLAndroidImageData data = ((ZLAndroidImageManager)ZLAndroidImageManager.Instance()).getImageData(imageToUse);
		if (data == null) {
			return;
		}

		final DisplayMetrics metrics = new DisplayMetrics();
		windowManager.getDefaultDisplay().getMetrics(metrics);
		final int maxHeight = metrics.heightPixels * 2 / 3;
		final int maxWidth = maxHeight * 2 / 3;
		final Bitmap coverBitmap = data.getBitmap(2 * maxWidth, 2 * maxHeight);
		if (coverBitmap == null) {
			return;
		}

		final boolean shouldSetWithForViewWithoutWidth = coverView.getLayoutParams().width <= 0;
		if (shouldSetWithForViewWithoutWidth) {
			coverView.getLayoutParams().width = maxWidth;
		}

		final boolean shouldSetHeightForViewWithoutHeight = coverView.getLayoutParams().height <= 0;
		if (shouldSetHeightForViewWithoutHeight) {
			coverView.getLayoutParams().height = maxHeight;
		}

		coverView.setVisibility(View.VISIBLE);
		coverView.setImageBitmap(coverBitmap);
	}

	private void setupBookInfo(Book book) {
		((TextView)findViewById(R.id.book_info_title)).setText(myResource.getResource("bookInfo").getValue());

		setupInfoPair(R.id.book_title, "title", book.getTitle());

		final StringBuilder buffer = new StringBuilder();
		for (Author author: book.authors()) {
			if (buffer.length() > 0) {
				buffer.append(", ");
			}
			buffer.append(author.DisplayName);
		}
		setupInfoPair(R.id.book_authors, "authors", buffer);

		final SeriesInfo series = book.getSeriesInfo();
		setupInfoPair(R.id.book_series, "series",
				(series == null) ? null : series.Name);
		String seriesIndexString = null;
		if (series != null && series.Index > 0) {
			if (Math.abs(series.Index - Math.round(series.Index)) < 0.01) {
				seriesIndexString = String.valueOf(Math.round(series.Index));
			} else {
				seriesIndexString = String.format("%.1f", series.Index);
			}
		}
		setupInfoPair(R.id.book_series_index, "indexInSeries", seriesIndexString);

		buffer.delete(0, buffer.length());
		final HashSet<String> tagNames = new HashSet<String>();
		for (Tag tag : book.tags()) {
			if (!tagNames.contains(tag.Name)) {
				if (buffer.length() > 0) {
					buffer.append(", ");
				}
				buffer.append(tag.Name);
				tagNames.add(tag.Name);
			}
		}
		setupInfoPair(R.id.book_tags, "tags", buffer);
		String language = book.getLanguage();
		if (!ZLLanguageUtil.languageCodes().contains(language)) {
			language = ZLLanguageUtil.OTHER_LANGUAGE_CODE;
		}
		setupInfoPair(R.id.book_language, "language", ZLLanguageUtil.languageName(language));
	}

	private void setupAnnotation(Book book) {
		final TextView titleView = (TextView)findViewById(R.id.book_info_annotation_title);
		final TextView bodyView = (TextView)findViewById(R.id.book_info_annotation_body);
		final String annotation = Library.getAnnotation(book.File);
		if (annotation == null) {
			titleView.setVisibility(View.GONE);
			bodyView.setVisibility(View.GONE);
		} else {
			titleView.setText(myResource.getResource("annotation").getValue());
			bodyView.setText(HtmlUtil.getHtmlText(annotation));
			bodyView.setMovementMethod(new LinkMovementMethod());
			bodyView.setTextColor(ColorStateList.valueOf(bodyView.getTextColors().getDefaultColor()));
		}
	}

	private void setupFileInfo(Book book) {
		((TextView)findViewById(R.id.file_info_title)).setText(myResource.getResource("fileInfo").getValue());

		setupInfoPair(R.id.file_name, "name", book.File.getPath());
		if (ENABLE_EXTENDED_FILE_INFO) {
			setupInfoPair(R.id.file_type, "type", book.File.getExtension());

			final ZLFile physFile = book.File.getPhysicalFile();
			final File file = physFile == null ? null : new File(physFile.getPath());
			if (file != null && file.exists() && file.isFile()) {
				setupInfoPair(R.id.file_size, "size", formatSize(file.length()));
				setupInfoPair(R.id.file_time, "time", formatDate(file.lastModified()));
			} else {
				setupInfoPair(R.id.file_size, "size", null);
				setupInfoPair(R.id.file_time, "time", null);
			}
		} else {
			setupInfoPair(R.id.file_type, "type", null);
			setupInfoPair(R.id.file_size, "size", null);
			setupInfoPair(R.id.file_time, "time", null);
		}
	}

	private String formatSize(long size) {
		if (size <= 0) {
			return null;
		}
		final int kilo = 1024;
		if (size < kilo) { // less than 1 kilobyte
			return myResource.getResource("sizeInBytes").getValue((int)size).replaceAll("%s", String.valueOf(size));
		}
		final String value;
		if (size < kilo * kilo) { // less than 1 megabyte
			value = String.format("%.2f", ((float)size) / kilo);
		} else {
			value = String.valueOf(size / kilo);
		}
		return myResource.getResource("sizeInKiloBytes").getValue().replaceAll("%s", value);
	}

	private String formatDate(long date) {
		if (date == 0) {
			return null;
		}
		return DateFormat.getDateTimeInstance().format(new Date(date));
	}

    @Override
    public void onWindowFocusChanged (boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        (findViewById(R.id.book_title)).requestFocus();
    }

	private void deleteBook(Book book) {
		if (book.File.getShortName().equals(FBReader.MINI_HELP_FILE_NAME)) {
			Toast.makeText(
					getApplicationContext(),
					getString(R.string.message_cannot_delete_guide),
					Toast.LENGTH_SHORT
			).show();
			return;
		}
		final AlertDialog dialog = new AlertDialog.Builder(this)
				.setTitle(book.getTitle())
				.setMessage(getString(R.string.message_confirm_remove_book))
				.setIcon(0)
				.setPositiveButton(getString(R.string.button_label_delete_book), deleteDialogListener)
				.setNegativeButton(getString(R.string.button_label_cancel), null)
				.create();
		dialog.setOnShowListener(new DialogInterface.OnShowListener() {
			@Override
			public void onShow(DialogInterface dialogInterface) {
				Button positiveButton = dialog
						.getButton(AlertDialog.BUTTON_POSITIVE);
				positiveButton.setContentDescription(getString(R.string.message_confirm_remove_book));
			}
		});
		dialog.show();
	}


	private DialogInterface.OnClickListener deleteDialogListener = new DialogInterface.OnClickListener() {

		@Override
		public void onClick(DialogInterface dialogInterface, int i) {
			final Book book = Book.getByFile(myFile);
			Library.Instance().removeBook(book, Library.REMOVE_FROM_DISK);
			((SQLiteBooksDatabase)SQLiteBooksDatabase.Instance()).clearBookStatus(book);
			DownloadedBookInfoActivity.this.setResult(RESULT_BOOK_DELETED, getIntent());
			DownloadedBookInfoActivity.this.finish();
		}
	};

}
