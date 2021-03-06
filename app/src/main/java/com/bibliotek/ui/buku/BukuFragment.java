package com.bibliotek.ui.buku;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.bibliotek.BuildConfig;
import com.bibliotek.R;
import com.bibliotek.adapter.BukuRecycleViewAdapter;
import com.bibliotek.api.bukuAPI;
import com.bibliotek.databinding.FragmentBukuBinding;
import com.bibliotek.model.Buku;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static com.android.volley.Request.Method.GET;

public class BukuFragment extends Fragment {

    BukuRecycleViewAdapter adapter;
    List<Buku> bukuList = new ArrayList<>();
    FloatingActionButton btnTambah;
    RecyclerView.LayoutManager mLayoutManager;

    FragmentBukuBinding binding;
    FragmentTransaction ft;
    View view;

    private FloatingActionButton btnPrint;
    private AlertDialog.Builder builder;
    private static final String TAG = "PdfCreatorActivity";
    final private int REQUEST_CODE_ASK_PERMISSIONS = 101;
    private File pdfFile;
    private PdfWriter writer;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_buku, container, false);
        view = binding.getRoot();

        getBuku();

        adapter = new BukuRecycleViewAdapter(inflater.getContext(), bukuList, new BukuRecycleViewAdapter.deleteItemListener() {
            @Override
            public void deleteItem(Boolean delete) {
                if(delete){
                    getBuku();
                }
            }
        });

        mLayoutManager = new LinearLayoutManager(getActivity());
        binding.bukuRv.setLayoutManager(new LinearLayoutManager(inflater.getContext()));
        binding.bukuRv.setHasFixedSize(true);
        binding.setData(adapter);

        btnTambah = view.findViewById(R.id.btnAddBuku);
        btnTambah.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(view.getContext(), addBuku.class);
                startActivityForResult(intent, 10005);
            }
        });

        btnPrint = view.findViewById(R.id.btnPrintBuku);
        btnPrint.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                builder = new AlertDialog.Builder(getContext());

                builder.setCancelable(false);
                builder.setMessage("Apakah anda yakin ingin mencetak laporan data buku ?");
                builder.setPositiveButton("Ya", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        try {
                            createPdfWrapper();
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        } catch (DocumentException e) {
                            e.printStackTrace();
                        }
                    }
                });
                builder.setNegativeButton("Tidak", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
                AlertDialog alert = builder.create();
                alert.show();
                alert.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.BLACK);
                alert.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.BLACK);
                alert.getButton(AlertDialog.BUTTON_NEGATIVE).setBackgroundColor(Color.WHITE);
                alert.getButton(AlertDialog.BUTTON_POSITIVE).setBackgroundColor(Color.WHITE);
            }
        });

        return view;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);
        if ((requestCode == 10005) && (resultCode == Activity.RESULT_OK)) {
            ft = getFragmentManager().beginTransaction();
            ft.detach(this).attach(this).commit();
        }
    }

    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        view.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {

                return true;
            }
        });
    }

    public void getBuku() {
        RequestQueue queue = Volley.newRequestQueue(view.getContext());

        final ProgressDialog progressDialog;
        progressDialog = new ProgressDialog(view.getContext());
        progressDialog.setMessage("loading....");
        progressDialog.setTitle("Menampilkan data buku");
        progressDialog.setProgressStyle(android.app.ProgressDialog.STYLE_SPINNER);
        progressDialog.show();

        final JsonObjectRequest stringRequest = new JsonObjectRequest(GET, bukuAPI.URL_INDEX
                , null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                progressDialog.dismiss();
                try {
                    JSONArray jsonArray = response.getJSONArray("data");

                    if(!bukuList.isEmpty())
                        bukuList.clear();

                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject jsonObject = (JSONObject) jsonArray.get(i);

                        String id           = jsonObject.optString("id");
                        String judul        = jsonObject.optString("judul");
                        String pengarang    = jsonObject.optString("pengarang");
                        String penerbit     = jsonObject.optString("penerbit");
                        String gambar       = jsonObject.optString("gambar");

                        if (gambar.equalsIgnoreCase("null")) {
                            gambar = "no-image.png";
                        }

                        Buku buku = new Buku(id, gambar, judul, pengarang, penerbit);
                        bukuList.add(buku);
                    }

                    adapter.notifyDataSetChanged();
                }catch (JSONException e){
                    e.printStackTrace();
                }

                Toast.makeText(view.getContext(), response.optString("message"),
                        Toast.LENGTH_SHORT).show();
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                progressDialog.dismiss();
                Toast.makeText(view.getContext(), error.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });

        queue.add(stringRequest);
    }

    @Override
    public void onResume() {
        super.onResume();
        getBuku();
    }

    private void createPdf() throws FileNotFoundException, DocumentException {
        File docsFolder = new File(Environment.getExternalStorageDirectory() + "/Download/");

        if (!docsFolder.exists()) {
            docsFolder.mkdir();
            Log.i(TAG, "Direktori baru untuk file pdf berhasil dibuat");
        }

        String pdfname = "LaporanDataBukuBibliotek"+".pdf";
        pdfFile = new File(docsFolder.getAbsolutePath(), pdfname);
        OutputStream output = new FileOutputStream(pdfFile);
        com.itextpdf.text.Document document = new com.itextpdf.text.Document(PageSize.A4);
        writer = PdfWriter.getInstance(document, output); document.open();

        Paragraph judul = new Paragraph(" LAPORAN DATA BUKU BIBLIOTEK \n\n",
                new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.TIMES_ROMAN, 16,
                        com.itextpdf.text.Font.BOLD, BaseColor.BLACK));
        judul.setAlignment(Element.ALIGN_CENTER);
        document.add(judul);

        PdfPTable tables = new PdfPTable(new float[]{16, 8});
        tables.getDefaultCell().setFixedHeight(50);
        tables.setTotalWidth(PageSize.A4.getWidth());
        tables.setWidthPercentage(100);
        tables.getDefaultCell().setBorder(Rectangle.NO_BORDER);

        PdfPCell cellSupplier = new PdfPCell();
        cellSupplier.setPaddingLeft(20);
        cellSupplier.setPaddingBottom(10);
        cellSupplier.setBorder(Rectangle.NO_BORDER);

        com.itextpdf.text.Font f = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.TIMES_ROMAN, 10,
                com.itextpdf.text.Font.NORMAL, BaseColor.BLACK);
        Paragraph Pembuka = new Paragraph("\nBerikut merupakan laporan data buku di Bibliotek : \n\n",f);
        Pembuka.setIndentationLeft(20);
        document.add(Pembuka);

        PdfPTable tableHeader = new PdfPTable(new float[]{1,2,4,4,4});
        tableHeader.getDefaultCell().setHorizontalAlignment(Element.ALIGN_CENTER);
        tableHeader.getDefaultCell().setVerticalAlignment(Element.ALIGN_MIDDLE);
        tableHeader.getDefaultCell().setFixedHeight(30);
        tableHeader.setTotalWidth(PageSize.A4.getWidth());
        tableHeader.setWidthPercentage(100);

        PdfPCell h0 = new PdfPCell(new Phrase("No"));
        h0.setHorizontalAlignment(Element.ALIGN_CENTER);
        h0.setPaddingBottom(5);

        PdfPCell h1 = new PdfPCell(new Phrase("ID Buku"));
        h1.setHorizontalAlignment(Element.ALIGN_CENTER);
        h1.setPaddingBottom(5);

        PdfPCell h2 = new PdfPCell(new Phrase("Judul Buku"));
        h2.setHorizontalAlignment(Element.ALIGN_CENTER);
        h2.setPaddingBottom(5);

        PdfPCell h3 = new PdfPCell(new Phrase("Pengarang Buku"));
        h3.setHorizontalAlignment(Element.ALIGN_CENTER);
        h3.setPaddingBottom(5);

        PdfPCell h4 = new PdfPCell(new Phrase("Penerbit Buku"));
        h4.setHorizontalAlignment(Element.ALIGN_CENTER);
        h4.setPaddingBottom(5);

        tableHeader.addCell(h0);
        tableHeader.addCell(h1);
        tableHeader.addCell(h2);
        tableHeader.addCell(h3);
        tableHeader.addCell(h4);

        PdfPCell[] cells = tableHeader.getRow(0).getCells();
        for (int j = 0; j < cells.length; j++) {
            cells[j].setBackgroundColor(new BaseColor(212, 170, 125));
        }
        document.add(tableHeader);

        PdfPTable tableData = new PdfPTable(new float[]{1,2,4,4,4});
        tableData.getDefaultCell().setHorizontalAlignment(Element.ALIGN_CENTER);
        tableData.getDefaultCell().setFixedHeight(30);
        tableData.setTotalWidth(PageSize.A4.getWidth());
        tableData.setWidthPercentage(100);
        tableData.getDefaultCell().setVerticalAlignment(Element.ALIGN_MIDDLE);

        int arrLength = bukuList.size();

        for(int x=0; x<arrLength; x++){
            for(int i=0;i<cells.length;i++){
                if(i==0){
                    tableData.addCell(String.valueOf(x+1));
                }
                else if(i==1){
                    tableData.addCell("Buku-" + bukuList.get(x).getId());
                }
                else if(i==2){
                    tableData.addCell(bukuList.get(x).getJudul());
                }
                else if(i==3){
                    tableData.addCell(bukuList.get(x).getPengarang());
                }
                else{
                    tableData.addCell(bukuList.get(x).getPenerbit());
                }
            }
        }
        document.add(tableData);

        com.itextpdf.text.Font h = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.TIMES_ROMAN, 10,
                com.itextpdf.text.Font.NORMAL);
        Date currentTime = Calendar.getInstance().getTime();
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMMM yyyy");

        String tglDicetak = sdf.format(currentTime);
        Paragraph P = new Paragraph("\nDicetak tanggal " + tglDicetak, h);
        P.setAlignment(Element.ALIGN_RIGHT);

        document.add(P);
        document.close();
        previewPdf();
    }

    private void showMessageOKCancel(String message, DialogInterface.OnClickListener okListener) {
        new AlertDialog.Builder(getContext())
                .setMessage(message)
                .setPositiveButton("OK", okListener)
                .setNegativeButton("Cancel", null)
                .create()
                .show();
    }

    private void createPdfWrapper() throws FileNotFoundException, DocumentException {
        int hasWriteStoragePermission = 0;

        if (hasWriteStoragePermission != PackageManager.PERMISSION_GRANTED) {
            if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.M) {
                if (!shouldShowRequestPermissionRationale(Manifest.permission.WRITE_CONTACTS)) {
                    showMessageOKCancel("Izinkan aplikasi untuk akses penyimpanan?", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CODE_ASK_PERMISSIONS);
                            }
                        }
                    });
                    return;
                }
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CODE_ASK_PERMISSIONS);
            }
            return;
        }
        else {
            createPdf();
        }
    }

    private void previewPdf() {
        PackageManager packageManager = getContext().getPackageManager();
        Intent testIntent = new Intent(Intent.ACTION_VIEW);
        testIntent.setType("application/pdf");

        List list = packageManager.queryIntentActivities(testIntent, PackageManager.MATCH_DEFAULT_ONLY);
        if (list.size() > 0) {
            Uri uri;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    uri = FileProvider.getUriForFile(getActivity(),BuildConfig.APPLICATION_ID + ".provider", pdfFile);
            }
            else {
                uri = Uri.fromFile(pdfFile);
            }

            Intent pdfIntent = new Intent(Intent.ACTION_VIEW);
            pdfIntent.setDataAndType(uri, "application/pdf");
            pdfIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            pdfIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            pdfIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            pdfIntent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
            pdfIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);

            getContext().grantUriPermission("package com.bibliotek.ui.buku", uri,
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(pdfIntent);
        }
        else {
            Toast.makeText(getContext(), "Unduh pembuka PDF untuk menampilkan file ini", Toast.LENGTH_SHORT).show();
        }
    }
}