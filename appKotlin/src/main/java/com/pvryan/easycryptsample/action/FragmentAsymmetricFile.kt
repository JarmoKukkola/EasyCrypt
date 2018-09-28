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

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.pvryan.easycrypt.ECKeys
import com.pvryan.easycrypt.asymmetric.ECAsymmetric
import com.pvryan.easycrypt.asymmetric.ECRSAKeyPairListener
import com.pvryan.easycrypt.symmetric.ECSymmetric
import com.pvryan.easycryptsample.R
import com.pvryan.easycryptsample.extensions.gone
import com.pvryan.easycryptsample.extensions.hide
import com.pvryan.easycryptsample.extensions.show
import com.pvryan.easycryptsample.extensions.snackLong
import com.pvryan.easycryptsample.extensions.toBase64String
import com.transitionseverywhere.TransitionManager
import kotlinx.android.synthetic.main.fragment_asymmetric_file.bExpandCollapsePrivateF
import kotlinx.android.synthetic.main.fragment_asymmetric_file.bExpandCollapsePublicF
import kotlinx.android.synthetic.main.fragment_asymmetric_file.buttonSelectDecryptF
import kotlinx.android.synthetic.main.fragment_asymmetric_file.buttonSelectEncryptF
import kotlinx.android.synthetic.main.fragment_asymmetric_file.buttonSignF
import kotlinx.android.synthetic.main.fragment_asymmetric_file.buttonVerifyF
import kotlinx.android.synthetic.main.fragment_asymmetric_file.container
import kotlinx.android.synthetic.main.fragment_asymmetric_file.edPasswordF
import kotlinx.android.synthetic.main.fragment_asymmetric_file.progressBarF
import kotlinx.android.synthetic.main.fragment_asymmetric_file.rlPrivateKeyTitleF
import kotlinx.android.synthetic.main.fragment_asymmetric_file.rlPublicKeyTitleF
import kotlinx.android.synthetic.main.fragment_asymmetric_file.tvPrivateKeyF
import kotlinx.android.synthetic.main.fragment_asymmetric_file.tvPublicKeyF
import kotlinx.android.synthetic.main.fragment_asymmetric_file.tvResultF
import kotlinx.android.synthetic.main.fragment_asymmetric_file.tvStatus
import timber.log.Timber
import java.io.File
import java.security.KeyPair
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.security.spec.InvalidKeySpecException

@SuppressLint("SetTextI18n")
class FragmentAsymmetricFile : Fragment() {

    private val _rCEncrypt = 2
    private val _rCDecrypt = 3
    private val _rCSign = 4
    private val _rCVerify = 5
    private val eCryptAsymmetric = ECAsymmetric()
    private val eCryptSymmetric = ECSymmetric()
    private val eCryptKeys = ECKeys()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_asymmetric_file, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val clipboard = activity?.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        rlPublicKeyTitleF.setOnLongClickListener {
            val data = ClipData.newPlainText("result", tvPublicKeyF.text)
            clipboard.primaryClip = data
            view.snackLong("Public key copied to clipboard")
            true
        }
        rlPrivateKeyTitleF.setOnLongClickListener {
            val data = ClipData.newPlainText("result", tvPrivateKeyF.text)
            clipboard.primaryClip = data
            view.snackLong("Secure private key copied to clipboard")
            true
        }

        buttonSelectEncryptF.setOnClickListener {
            if (edPasswordF.text.toString().isBlank()) {
                view.snackLong("Password cannot be empty")
                return@setOnClickListener
            }
            selectFile(_rCEncrypt)
        }
        buttonSelectDecryptF.setOnClickListener {
            if (edPasswordF.text.toString().isBlank()) {
                view.snackLong("Password cannot be empty")
                return@setOnClickListener
            }
            if (tvPrivateKeyF.text.isBlank()) {
                view.snackLong("Encrypt first to generate private key")
                return@setOnClickListener
            }
            selectFile(_rCDecrypt)
        }
        buttonSignF.setOnClickListener {
            if (edPasswordF.text.toString().isBlank()) {
                view.snackLong("Password cannot be empty")
                return@setOnClickListener
            }
            selectFile(_rCSign)
        }
        buttonVerifyF.setOnClickListener {
            if (tvPublicKeyF.text.isBlank()) {
                view.snackLong("Sign first to generate public key")
                return@setOnClickListener
            }
            selectFile(_rCVerify)
        }

        rlPrivateKeyTitleF.setOnClickListener {
            if (tvPrivateKeyF.visibility == View.GONE) {
                bExpandCollapsePrivateF.animate().rotation(180f).setDuration(200).start()
                tvPrivateKeyF.show()
            } else {
                bExpandCollapsePrivateF.animate().rotation(0f).setDuration(200).start()
                tvPrivateKeyF.gone()
            }
            TransitionManager.beginDelayedTransition(rlPrivateKeyTitleF.parent as ViewGroup)
        }

        rlPublicKeyTitleF.setOnClickListener {
            if (tvPublicKeyF.visibility == View.GONE) {
                bExpandCollapsePublicF.animate().rotation(180f).setDuration(200).start()
                tvPublicKeyF.show()
            } else {
                bExpandCollapsePublicF.animate().rotation(0f).setDuration(200).start()
                tvPublicKeyF.gone()
            }
            TransitionManager.beginDelayedTransition(rlPublicKeyTitleF.parent as ViewGroup)
        }
    }

    private fun selectFile(requestCode: Int) {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "*/*"
        startActivityForResult(intent, requestCode)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        val fis = data?.data?.let { context?.contentResolver?.openInputStream(it) } ?: return

        if (resultCode == Activity.RESULT_OK) {

            val password = edPasswordF.text.toString()

            when (requestCode) {

                _rCEncrypt -> {
                    progressBarF.show()
                    tvStatus.text = getString(R.string.tv_status_encrypting)
                    eCryptKeys.genRSAKeyPair(object : ECRSAKeyPairListener {
                        override fun onGenerated(keyPair: KeyPair) {
                            val publicKey = keyPair.public as RSAPublicKey
                            tvPublicKeyF.text = publicKey.encoded.toBase64String()

                            val privateKey = keyPair.private as RSAPrivateKey
                            // Symmetrically encrypt private key
                            eCryptSymmetric.encrypt<String>(privateKey.encoded.toBase64String(), password)
                                    .onSuccess { result ->
                                        tvPrivateKeyF.text = result
                                        eCryptAsymmetric
                                                .encrypt<File>(fis, publicKey)
                                                .onSuccess(this@FragmentAsymmetricFile::onSuccess)
                                                .onFailure(this@FragmentAsymmetricFile::onFailure)
                                                .onProgress(this@FragmentAsymmetricFile::onProgress)
                                    }
                                    .onFailure { message, _ ->
                                        progressBarF.hide()
                                        tvStatus.text = getString(R.string.tv_status_idle)
                                        tvPrivateKeyF.text = "Error while encrypting private key. $message"
                                        container.snackLong("Error while encrypting private key. $message")
                                    }
                        }

                        override fun onFailure(message: String, e: Exception) {
                            progressBarF.hide()
                            tvStatus.text = getString(R.string.tv_status_idle)
                            container.snackLong("Error: $message")
                        }
                    })
                }

                _rCDecrypt -> {
                    tvStatus.text = getString(R.string.tv_status_decrypting)
                    // Decrypt private key
                    eCryptSymmetric
                            .decrypt<String>(tvPrivateKeyF.text, password)
                            .onSuccess { result ->
                                try {
                                    val privateKey = eCryptKeys.genRSAPrivateKeyFromBase64(result)
                                    // Decrypt user file
                                    eCryptAsymmetric
                                            .decrypt<File>(fis, privateKey)
                                            .onProgress(this@FragmentAsymmetricFile::onProgress)
                                            .onSuccess(this@FragmentAsymmetricFile::onSuccess)
                                            .onFailure(this@FragmentAsymmetricFile::onFailure)

                                    progressBarF.show()
                                } catch (e: IllegalArgumentException) {
                                    Timber.d(e)
                                    onFailure("Not a valid base64 string", e)
                                } catch (e: InvalidKeySpecException) {
                                    onFailure("Not a valid private key", e)
                                }
                            }
                            .onFailure { message, _ ->
                                tvStatus.text = getString(R.string.tv_status_idle)
                                container.snackLong("Error while decrypting private key. $message")
                            }
                }

                _rCSign -> {
                    tvStatus.text = getString(R.string.tv_status_signing)
                    progressBarF.show()

                    val sigFile = File(Environment.getExternalStorageDirectory(),
                            "ECryptSample/sample.sig")
                    if (sigFile.exists()) sigFile.delete()

                    eCryptKeys.genRSAKeyPair(object : ECRSAKeyPairListener {

                        override fun onGenerated(keyPair: KeyPair) {
                            tvPublicKeyF.text = (keyPair.public as RSAPublicKey).encoded.toBase64String()
                            val privateKey = keyPair.private as RSAPrivateKey

                            // Encrypt private key
                            eCryptSymmetric
                                    .encrypt<String>(privateKey.encoded.toBase64String(), password)
                                    .onSuccess { result ->
                                        eCryptAsymmetric
                                                .sign(fis, privateKey, sigFile)
                                                .onProgress(this@FragmentAsymmetricFile::onProgress)
                                                .onSuccess(this@FragmentAsymmetricFile::onSuccess)
                                                .onFailure(this@FragmentAsymmetricFile::onFailure)
                                        tvPrivateKeyF.text = result
                                    }
                                    .onFailure { message, _ ->
                                        progressBarF.hide()
                                        tvStatus.text = getString(R.string.tv_status_idle)
                                        tvPrivateKeyF.text = "Error while encrypting private key. $message"
                                        container.snackLong("Error while encrypting private key. $message")
                                    }
                        }

                        override fun onFailure(message: String, e: Exception) {
                            progressBarF.hide()
                            tvStatus.text = getString(R.string.tv_status_idle)
                            container.snackLong("Failed to generate RSA key pair. Try again.")
                        }
                    })
                }

                _rCVerify -> {
                    progressBarF.show()
                    tvStatus.text = getString(R.string.tv_status_verifying)
                    try {
                        val publicKey = eCryptKeys.genRSAPublicKeyFromBase64(tvPublicKeyF.text.toString())
                        eCryptAsymmetric.verify(
                                fis,
                                publicKey,
                                File(Environment.getExternalStorageDirectory(), "ECryptSample/sample.sig")
                        ).onSuccess { verified ->
                            progressBarF.hide()
                            if (verified) tvResultF.text = getString(R.string.msg_valid)
                            else tvResultF.text = getString(R.string.msg_invalid)
                            tvStatus.text = getString(R.string.tv_status_idle)
                        }.onFailure { message, _ ->
                            progressBarF.hide()
                            tvStatus.text = getString(R.string.tv_status_idle)
                            container.snackLong("Error: $message")
                        }
                    } catch (e: IllegalArgumentException) {
                        Timber.d(e)
                        container.snackLong("Not a valid base64 string")
                        tvStatus.text = getString(R.string.tv_status_idle)
                    } catch (e: InvalidKeySpecException) {
                        container.snackLong("Not a valid private key")
                        tvStatus.text = getString(R.string.tv_status_idle)
                    }
                }
            }
        }
    }

    private var maxSet = false
    fun onProgress(newBytes: Int, bytesProcessed: Long, totalBytes: Long) {
        if (totalBytes > -1) {
            if (!maxSet) {
                progressBarF.isIndeterminate = false
                progressBarF.max = (totalBytes / 1024).toInt()
                maxSet = true
            }
            progressBarF.progress = (bytesProcessed / 1024).toInt()
        }
    }

    fun <T> onSuccess(result: T) {
        progressBarF.hide()
        tvStatus.text = getString(R.string.tv_status_idle)
        tvResultF.text = resources.getString(
                R.string.success_result_to_file,
                (result as File).absolutePath)
    }

    fun onFailure(message: String, e: Throwable) {
        progressBarF.hide()
        tvStatus.text = getString(R.string.tv_status_idle)
        container.snackLong("Error: $message")
    }

    companion object {
        fun newInstance(): Fragment = FragmentAsymmetricFile()
    }
}