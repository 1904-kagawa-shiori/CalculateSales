package jp.alhinc.calculate_sales;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CalculateSales {

	// 支店定義ファイル名
	private static final String FILE_NAME_BRANCH_LST = "branch.lst";

	// 支店別集計ファイル名
	private static final String FILE_NAME_BRANCH_OUT = "branch.out";

	//商品定義ファイル名
	private static final String FILE_NAME_COMMODITY_LST = "commodity.lst";

	//商品別集計ファイル名
	private static final String FILE_NAME_COMMODITY_OUT = "commodity.out";

	// エラーメッセージ
	private static final String UNKNOWN_ERROR = "予期せぬエラーが発生しました";
	private static final String FILE_NOT_EXIST = "が存在しません";
	private static final String FILE_INVALID_FORMAT = "のフォーマットが不正です";
	private static final String NOT_CONSECUTIVE_NUMBERS = "売上ファイル名が連番になっていません";
	private static final String TOTAL_AMOUNT_EXCEEDED_10_DIGITS = "合計金額が10桁を超えました";

	/**
	 * メインメソッド
	 *
	 * @param コマンドライン引数
	 */
	public static void main(String[] args) {

		//エラー処理3-1 コマンドライン引数が1つ設定されているかチェック
		if(args.length != 1) {
			System.out.println(UNKNOWN_ERROR);
			return;
		}


		// 支店コードと支店名を保持するMap
		Map<String, String> branchNames = new HashMap<>();
		// 支店コードと売上金額を保持するMap
		Map<String, Long> branchSales = new HashMap<>();

		//機能追加：商品情報を格納するためのMap
		//商品コードと商品名を保持するMap
		Map<String, String> commodityNames = new HashMap<>();
		//商品コードと商品別売上金額を保持するMap
		Map<String, Long> commoditySales = new HashMap<>();


		// 支店定義ファイル読み込み処理
		if(!readFile(args[0], FILE_NAME_BRANCH_LST, "^[0-9]{3}$", "支店定義ファイル", branchNames, branchSales)) {
			return;
		}

		//商品定義ファイル読み込み処理
		if(!readFile(args[0], FILE_NAME_COMMODITY_LST, "^[a-zA-Z0-9]{8}$", "商品定義ファイル", commodityNames, commoditySales)) {
			return;
		}


		// ※ここから集計処理を作成してください。(処理内容2-1、2-2)
		//listFilesメソッドにて指定したディレクトリに含まれる対象ファイル情報を格納
		//listFiles() メソッド：File オブジェクトが表すディレクトリ内のすべてのファイルとサブディレクトリを抽象パス名として返す
		//listFiles() メソッドの戻り値は、 File オブジェクトの配列 (File[]) になる
		File[] files = new File(args[0]).listFiles();

		//対象の売上ファイルを格納するためのrcdFilesリストを宣言
		List<File> rcdFiles = new ArrayList<>();

		//指定したパスに存在するFile オブジェクトの総数、つまりそのパス先に存在するファイルの数だけ処理を繰り返す
		for(int i = 0; i < files.length ; i++) {
			//files[i].getName()でファイル名取得できる
			String fileName = files[i].getName();

			//.isFile→ファイルかどうかを調べる
			//拡張子rcdかつファイル名が数字8桁のファイル名だったら、List(ArrayList)に追加する
			//正規表現　.の部分　そのままだと設定が甘いので、修正(.は任意の1文字にマッチするメタ文字のため)
			//"[.]"や"\\."で設定してあげるとよい
			//エラー処理3-2 売上ファイルがファイルか(ディレクトリじゃないか)を確認
			//isFileメソッドで、ファイルの場合はtrue、それ以外の場合はfalseを返す
			if(files[i].isFile() && fileName.matches("^[0-9]{8}\\.rcd$")) {
				rcdFiles.add(files[i]);
			}
		}

		Collections.sort(rcdFiles);

		//エラー処理2-1
		//売上ファイルが連番かチェック
		//比較回数は売上ファイルの数よりも1回少ないため、売上ファイルのリストの数-1回繰り返す
		for(int i = 0; i < rcdFiles.size() -1; i++) {
			//売上ファイルのうち若い数字の方を格納する変数を準備
			//売上ファイル名は、rcdFiles.get(i)→i番目のrcdFiles .getName()→その売上ファイルの名前 で取得できる
			//Substringメソッドで、引数で指定した範囲の文字列を切り出して返す(今回の場合、先頭の数字8文字)
			//.substringのままだと文字列なので、parseIntを使って型変換する
			int former = Integer.parseInt(rcdFiles.get(i).getName().substring(0, 8));
			//売上ファイルのうち若くない数字の方を格納する変数を準備
			int latter = Integer.parseInt(rcdFiles.get(i + 1).getName().substring(0, 8));

			//若くない方と若い方の数字を比較して、差が1じゃないかどうかを判定
			if((latter - former) != 1) {
				//差が1でないなら想定外なので、エラーメッセージを表示する
				System.out.println(NOT_CONSECUTIVE_NUMBERS);
				return;
			}
		}

		//売上ファイル読み込み
		BufferedReader br = null;

		//rcdFilesに複数の売上ファイルを格納しているので、その数だけ処理を繰り返す
		for(int i = 0; i < rcdFiles.size(); i++) {
			try {
				//売上ファイル「rcdFiles」を読み込む
				FileReader fr = new FileReader(rcdFiles.get(i));
				br = new BufferedReader(fr);
				//br = new BufferedReader(new FileReader(rcdFiles.get(i)));

				//売上ファイルの中身を入れるためのリスト「fileContents」を作成する→ここの理解が抜けてた
				ArrayList<String> fileContents = new ArrayList<>();

				//rcdFilesの1行目に支店コード、2行目に商品コード、3行目に売上金額が入っている
				//最後の行まで読み込み、fileContentsに要素を1つずつ追加していく
				String line = "";
				while((line = br.readLine()) != null) {
					fileContents.add(line);
				}

				//行数チェック、支店コードチェックの際に対象売上ファイル名を表現するための変数を準備
				String fileName = rcdFiles.get(i).getName();

				//エラー処理2-4.
				//売上ファイルフォーマットの行数チェック(1行目に支店コード、2行目に商品コード、3行目に売上金額が記載されているか)
				if(fileContents.size() != 3){
					System.out.println(fileName +"のフォーマットが不正です");
					return;
				}

				//branchCode変数に、fileContentsリストの1つめの要素、つまり支店コードを入れる
				//branchCodeは、後でMapの支店コードをKeyにValueを取り出す際に使用する
				String branchCode = fileContents.get(0);

				//commodityCode変数に、fileContentsリストの2つめの要素、つまり商品コードを入れる
				//commodityCodeは、後でMapの商品コードをkeyに、Valueを取り出す際に使用する
				String commodityCode = fileContents.get(1);//2番目の要素であっているはず


				//エラー処理2-3. 支店コードのチェック
				//売上ファイルの支店コードが支店定義ファイルに存在するか確認
				//つまり支店情報を保持しているMap「branchNames」に、売上ファイルの支店コードが存在しているか確認
				//containsKeyメソッドで、引数で指定したKeyがMapに存在する場合はtrue,存在しない場合はfalseを返す
				if(!branchNames.containsKey(branchCode)) {
					//売上ファイルの支店コードが存在していなかったらエラーメッセージを表示する
					System.out.println(fileName + "の支店コードが不正です");
					return;
				}

				//商品コードの存在チェックを追加
				if(!commodityNames.containsKey(commodityCode)) {
					System.out.println(fileName + "の商品コードが不正です");
					return;
				}

				//エラー処理3-3 売上金額が数字なのかチェック
				//売上金額を読み込む際に、売上金額をLongに変換するため、売上金額が数字なのかを事前に確認する必要がある
				//^[0-9]+$: 123, 5, 0 など、文字列全体が1つ以上の数字で構成されている場合にマッチする
				//^\d+$ でも表せるが、\dはUnicodeの数字にもマッチする可能性があり、半角数字のみに限定したい場合は不向き
				//+ の代わりに * を使用すると、0回以上の繰り返し＝空文字列もマッチしてしまうので非推奨
				if(!fileContents.get(2).matches("^[0-9]+$")) {
					System.out.println(UNKNOWN_ERROR);
					return;
				}

				//ファイルから読み込んだ情報は一律で文字列として扱われるので、Long型に変換する
				//long型のfileSale変数に、fileContentsリストの3つめの要素、つまり売上金額を入れる
				long fileSale = Long.parseLong(fileContents.get(2));


				//読み込んだ売上金額を加算する
				//Map「branchSales」のKey(支店コード)を渡してバリュー(売上金額)を取り出し、longに変換した売上金額(fileSale)を加算する
				Long branchSaleAmount = branchSales.get(branchCode) + fileSale;

				//Map「」のkey(商品コード)を渡してバリュー(売上金額)を取り出し、longに変換した売上金額(fileSale)を加算する
				Long commoditySaleAmount = commoditySales.get(commodityCode) + fileSale;

				//エラー処理2-2. 売上金額の合計が11桁以上になっていないかチェック
				if(branchSaleAmount >= 10000000000L || commoditySaleAmount >= 10000000000L) {
					//売上金額が11桁以上の場合はエラーメッセージを表示する
					System.out.println(TOTAL_AMOUNT_EXCEEDED_10_DIGITS);
					return;
				}


				//加算した売上金額をMap「branchSales」に追加
				branchSales.put(branchCode, branchSaleAmount);
				//加算した売上金額を「commoditySales」に追加
				commoditySales.put(commodityCode, commoditySaleAmount);

			} catch(IOException e)  {
				System.out.println(UNKNOWN_ERROR);
				return;
			}finally {
				// ファイルを開いている場合
				if(br != null) {
					try {
						// ファイルを閉じる
						br.close();
					} catch(IOException e) {
						System.out.println(UNKNOWN_ERROR);
						return;
					}
				}
			}

		}


		// 支店別集計ファイル書き込み処理
		if(!writeFile(args[0], FILE_NAME_BRANCH_OUT, branchNames, branchSales)) {
			return;
		}
		//商品別集計ファイル書き込み処理
		if(!writeFile(args[0], FILE_NAME_COMMODITY_OUT, commodityNames, commoditySales)) {
			return;
		}

	}

	/**
	 * 支店定義ファイル読み込み処理
	 *
	 * @param フォルダパス
	 * @param ファイル名
	 * @param 正規表現
	 * @param エラーメッセージ出力時の名前
	 * @param 支店コードと支店名を保持するMap
	 * @param 支店コードと売上金額を保持するMap
	 * @return 読み込み可否
	 */
	//Map部分の引数について、branchames→namesへ、branchSales→salesに変更した
	private static boolean readFile(String path, String fileName, String regularExpression, String errorType, Map<String, String> names, Map<String, Long> sales) {
		BufferedReader br = null;

		try {
			File file = new File(path, fileName);

			//エラー処理1-1 支店定義ファイルが存在しない場合、エラー出力
			if(!file.exists()) {
				System.out.println(errorType + FILE_NOT_EXIST);//errorTypeを追加
				//falseを返すようにする
				return false;
			}

			FileReader fr = new FileReader(file);
			br = new BufferedReader(fr);

			String line;
			String[] items;

			// 一行ずつ読み込む
			while((line = br.readLine()) != null) {
				// 処理内容1-2
				//splitにて文字列をカンマで分割
				items = line.split(",");

				//エラー処理1-2 支店定義ファイルフォーマットチェック
				//1行に支店コードと支店名が「,」で区切られて記載されているか、items配列の要素の長さでチェック
				//支店コードが数字3桁かをチェック
				if((items.length != 2) || (!items[0].matches(regularExpression))){//"^[0-9]{3}$"をregularExpressionに修正
					System.out.println(errorType + FILE_INVALID_FORMAT);
					//falseを返すようにする
					return false;
				}


				//Mapに支店コードと支店名を追加する
				//支店コードは配列の0番目、支店名は配列の1番目に入っている
				names.put(items[0], items[1]);

				//Mapに支店コードと売上金額を追加する
				//売上金額はまだ不明だがシステム上前日の売上金額を繰り越さないため、「0」円で追加
				//売上金額はlong型　単純に0と入れるだけではint型で扱われてしまう
				//Longで固定値を追加する場合は、明示的にLongの整数と示す(=数字に大文字Lをつける)orキャストしてもいい((データ型)式みたいな書き方でも可)
				sales.put(items[0], 0L);
			}

		} catch(IOException e) {
			System.out.println(UNKNOWN_ERROR);
			return false;
		} finally {
			// ファイルを開いている場合
			if(br != null) {
				try {
					// ファイルを閉じる
					br.close();
				} catch(IOException e) {
					System.out.println(UNKNOWN_ERROR);
					return false;
				}
			}
		}
		return true;
	}

	/**
	 * 支店別集計ファイル書き込み処理
	 *
	 * @param フォルダパス
	 * @param ファイル名
	 * @param 支店コードと支店名を保持するMap
	 * @param 支店コードと売上金額を保持するMap
	 * @return 書き込み可否
	 */
	private static boolean writeFile(String path, String fileName, Map<String, String> names, Map<String, Long> sales) {
		// 処理内容3-1
		BufferedWriter bw = null;

		try {
			File file = new File(path, fileName);
			bw = new BufferedWriter(new FileWriter(file));

			//変数keyに、Map「branchNames」から取得したキーを代入する
			//keySetメソッドで、キーの一覧を取り出す
			//拡張for文で、1つめのキーが取得出来たら3つ目、というように次々Keyに上書きされていく
			for (String key : names.keySet()) {
				//出力内容：(各支店の)支店コード,支店名,売上金額
				//一行前で、変数keyに支店コードの値が入ったので、それを活用する
				bw.write(key + "," + names.get(key) + "," + sales.get(key));
				//ファイルへ書き込む際に、明示的に改行させる
				bw.newLine();
			}

		} catch(IOException e) {
			System.out.println(UNKNOWN_ERROR);
			return false;
		} finally {
			// 書き込みファイルがnullでない場合
			if(bw != null) {
				try {
					//closeメソッドでファイルへのデータ出力の流れを止める
					bw.close();
				} catch(IOException e) {
					System.out.println(UNKNOWN_ERROR);
					return false;
				}
			}
		}
		return true;
	}

}
