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
import com.pvryan.easycrypt.ECResultListener
import com.pvryan.easycrypt.symmetric.ECSymmetric
import com.pvryan.easycryptsample.R
import com.pvryan.easycryptsample.extensions.hide
import com.pvryan.easycryptsample.extensions.show
import com.pvryan.easycryptsample.extensions.snackLong
import com.pvryan.easycryptsample.extensions.snackShort
import kotlinx.android.synthetic.main.fragment_symmetric_file.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.support.v4.runOnUiThread
import java.io.File

class FragmentSymmetricFile :Fragment(),AnkoLogger,ECResultListener {

    private val _rCEncrypt = 2
    private val _rCDecrypt = 3
    private val eCryptSymmetric = ECSymmetric()

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_symmetric_file, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        buttonSelectEncryptF.setOnClickListener {
            if (edPasswordF.text.toString() != "")
                selectFile(_rCEncrypt)
            else view.snackShort("Password cannot be empty!")
        }
        buttonSelectDecryptF.setOnClickListener {
            if (edPasswordF.text.toString() != "")
                selectFile(_rCDecrypt)
            else view.snackShort("Password cannot be empty!")
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

            val fis = context?.contentResolver?.openInputStream(data?.data!!)

            progressBarF.show()

            when (requestCode) {
                _rCEncrypt -> {
                    tvStatus.text = resources.getString(R.string.tv_status_encrypting)
                    eCryptSymmetric.encrypt(fis, edPasswordF.text.toString(), this)
                }
                _rCDecrypt -> {
                    tvStatus.text = resources.getString(R.string.tv_status_decrypting)
                    eCryptSymmetric.decrypt(fis, edPasswordF.text.toString(), this)
                }
            }
        }
    }

    private var maxSet = false
    override fun onProgress(newBytes: Int, bytesProcessed: Long, totalBytes: Long) {
        if (totalBytes > -1) {
            runOnUiThread {
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
        runOnUiThread {
            progressBarF.hide()
            tvStatus.text = getString(R.string.tv_status_idle)
            tvResultF.text = resources.getString(
                    R.string.success_result_to_file,
                    (result as File).absolutePath)
        }
    }

    override fun onFailure(message: String, e: Exception) {
        e.printStackTrace()
        runOnUiThread {
            progressBarF.hide()
            tvStatus.text = getString(R.string.tv_status_idle)
            llContentSFile.snackLong("Error: $message")
        }
    }

    companion object {
        fun newInstance():Fragment = FragmentSymmetricFile()
    }
}
