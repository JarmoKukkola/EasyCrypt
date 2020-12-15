/*
 * Copyright 2018 Priyank Vasa
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.pvryan.easycryptsample.action

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import com.pvryan.easycrypt.ECResultListener
import com.pvryan.easycrypt.hash.ECHash
import com.pvryan.easycrypt.hash.ECHashAlgorithms
import com.pvryan.easycryptsample.R
import com.pvryan.easycryptsample.extensions.hide
import com.pvryan.easycryptsample.extensions.show
import com.pvryan.easycryptsample.extensions.snackLong
import kotlinx.android.synthetic.main.fragment_hash_file.*
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.support.v4.onUiThread

class FragmentHashFile :Fragment(),AnkoLogger,ECResultListener {

    private val _rCHash = 2
    private val eCryptHash = ECHash()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_hash_file, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val hashAdapter: ArrayAdapter<ECHashAlgorithms> = ArrayAdapter(view.context,
                android.R.layout.simple_spinner_item,
                arrayListOf(ECHashAlgorithms.SHA_512,
                        ECHashAlgorithms.SHA_384,
                        ECHashAlgorithms.SHA_256,
                        ECHashAlgorithms.SHA_224,
                        ECHashAlgorithms.SHA_1,
                        ECHashAlgorithms.MD5))
        hashAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerHashF.adapter = hashAdapter

        buttonSelectHashF.setOnClickListener {
            selectFile(_rCHash)
        }
    }

    private fun selectFile(requestCode: Int) {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "*/*"
        startActivityForResult(intent, requestCode)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        if (resultCode == Activity.RESULT_OK) {

            val fis = context?.contentResolver?.openInputStream(data!!.data!!)

            progressBarF.show()

            when (requestCode) {
                _rCHash -> {
                    tvStatus.text = getString(R.string.tv_status_hashing)
                    eCryptHash.calculate(fis, spinnerHashF.selectedItem as ECHashAlgorithms, this)
                }
            }
        }
    }

    private var maxSet = false
    override fun onProgress(newBytes: Int, bytesProcessed: Long, totalBytes: Long) {
        if (totalBytes > -1) {
            onUiThread {
                if (!maxSet) {
                    progressBarF.isIndeterminate = false
                    progressBarF.max = (totalBytes / 1024).toInt()
                    maxSet = true
                }
                progressBarF.progress = (bytesProcessed / 1024).toInt()
            }
        }
    }

    override fun <T> onSuccess(result: T) {
        onUiThread {
            progressBarF.hide()
            tvStatus.text = getString(R.string.tv_status_idle)
            tvResultF.text = result as String
        }
    }

    override fun onFailure(message: String, e: Exception) {
        e.printStackTrace()
        onUiThread {
            progressBarF.hide()
            tvStatus.text = getString(R.string.tv_status_idle)
            llContentHFile.snackLong("Error: $message")
        }
    }

    companion object {
        fun newInstance():Fragment = FragmentHashFile()
    }
}
