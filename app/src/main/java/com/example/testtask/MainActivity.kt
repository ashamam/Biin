package com.example.testtask

import android.content.Context
import android.os.Bundle
import android.text.TextWatcher
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.testtask.databinding.ActivityMainBinding
import okhttp3.*
import java.io.IOException
import com.google.gson.Gson
import android.text.Editable
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView


class MainActivity : AppCompatActivity() {


    private lateinit var binding: ActivityMainBinding
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: HistoryAdapter
    private lateinit var historyList: MutableList<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        historyList = mutableListOf()

        // Инициализация RecyclerView
        recyclerView = findViewById(R.id.recyclerViewHistory)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Инициализация и установка адаптера
        adapter = HistoryAdapter(historyList)
        recyclerView.adapter = adapter

    }

    override fun onResume() {

        val gson = Gson()

        val client = OkHttpClient()

        val input = binding.BINIIN

        val editText = findViewById<EditText>(R.id.BIN_IIN)

        val inputMethodManager = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager

        editText.addTextChangedListener(object: TextWatcher{

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }
            private var isFormatting = false

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Проверяем, чтобы форматирование не вызывало дополнительные изменения
                if (isFormatting) {
                    return
                }
                    // Удаляем предыдущие разделители из текста
                    val input = s.toString().replace(" ", "")

                    // Разделяем строку на группы по 4 символа с пробелом
                    val formattedText = input.chunked(4).joinToString(" ")

                    // Устанавливаем отформатированный текст в поле ввода
                    isFormatting = true
                    editText.setText(formattedText)
                    editText.setSelection(formattedText.length)
                    isFormatting = false
            }

            override fun afterTextChanged(s: Editable?) {
            }

        })

        editText.setOnClickListener{

            recyclerView.visibility = View.VISIBLE

        }

        editText.setOnEditorActionListener{ textView, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                // Действия при завершении ввода

                addToHistory(input.text.toString())

                val request = Request.Builder()
                    .url("https://lookup.binlist.net/${input.text.toString().replace(" ", "")}")
                    .get()
                    .build()

                inputMethodManager.hideSoftInputFromWindow(input.windowToken, 0)

                client.newCall(request).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        // Обработка ошибки при выполнении запроса
                    }

                    override fun onResponse(call: Call, response: Response) {

                        val responseBody = response.body?.string()

                        val cardInfo = gson.fromJson(responseBody, CardInfo::class.java)

                            if (cardInfo == null) {
                                runOnUiThread {
                                    Toast.makeText(
                                        this@MainActivity, "По данному BIN/INN ничего не найдено",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                            // Обновление пользовательского интерфейса в основном потоке
                            else {
                            runOnUiThread {
                                binding.SchemeName.setText(cardInfo.scheme)
                                binding.BrandName.setText(cardInfo.brand)
                                binding.Length.setText(cardInfo.number.length.toString())
                                binding.LUHN.setText(if (cardInfo.number.luhn == true) "Yes" else "No")
                                binding.TypeName.setText(cardInfo.type)
                                binding.PrepaidText.setText(if (cardInfo.prepaid == true) "Yes" else "No")
                                binding.textView.setText(cardInfo.country.name)
                                binding.Latitude.setText(cardInfo.country.latitude.toString())
                                binding.Longitude.setText(cardInfo.country.longitude.toString())
                                binding.PhoneNumber.setText(cardInfo.bank.phone)
                                binding.URL.setText(cardInfo.bank.url)
                                binding.Address.setText(cardInfo.bank.name + ", " + cardInfo.bank.city)
                            }
                        }
                    }
                })

                true
            }
                else {
                false
            }
        }


        super.onResume()
    }

    private fun addToHistory(input: String) {
        historyList.add(input)
        adapter.notifyItemInserted(historyList.size - 1)
    }
}
